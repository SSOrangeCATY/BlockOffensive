package com.phasetranscrystal.blockoffensive.minimap;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Task 9 source guards: BO must not reimplement FPSMatch minimap infrastructure,
 * and common extension registration must not pull client/HUD/render classes.
 */
class CSMinimapIntegrationSourceGuardTest {
    private static final Path BO_MINIMAP = Path.of("src/main/java/com/phasetranscrystal/blockoffensive/minimap");
    private static final Path BO_MAIN = Path.of("src/main/java/com/phasetranscrystal/blockoffensive/BlockOffensive.java");

    @Test
    void boMinimapPackageDoesNotReimplementFpsmatchStreamingOrFormat() throws Exception {
        List<String> forbidden = List.of(
                "MarkerStreamManager",
                "ClientMarkerStore",
                "MinimapOpcode",
                "Fpsmap",
                "Fpsmapc",
                "HudSafeAreaResolver",
                "WorldBake",
                "LayerComposer",
                "MinimapCodecs"
        );
        try (Stream<Path> paths = Files.walk(BO_MINIMAP)) {
            List<Path> files = paths.filter(p -> p.toString().endsWith(".java")).toList();
            assertFalse(files.isEmpty());
            for (Path file : files) {
                String src = Files.readString(file, StandardCharsets.UTF_8);
                for (String token : forbidden) {
                    assertFalse(src.contains(token), file.getFileName() + " must not reimplement " + token);
                }
            }
        }
    }

    @Test
    void commonExtensionAndMountAvoidClientPackages() throws Exception {
        for (String rel : List.of(
                "BlockOffensiveMinimapExtension.java",
                "BlockOffensiveMinimapMount.java",
                "CSGameObjectiveTracker.java",
                "CSMinimapVisibilityPolicy.java",
                "CSMinimapIdentityResolver.java",
                "CSMapMinimapMarkerProvider.java",
                "CSGameMinimapRegionProvider.java"
        )) {
            String src = Files.readString(BO_MINIMAP.resolve(rel), StandardCharsets.UTF_8);
            assertFalse(src.contains("net.minecraft.client"), rel + " must stay common/server pure");
            assertFalse(src.contains("com.phasetranscrystal.blockoffensive.client"), rel + " must not import BO client");
            assertFalse(src.contains("GuiGraphics"), rel + " must not reference render GUI");
        }
        String main = Files.readString(BO_MAIN, StandardCharsets.UTF_8);
        int register = main.indexOf("BlockOffensiveMinimapExtension.register()");
        assertTrue(register >= 0);
        // registration is in commonSetup, not DistExecutor client-only block
        assertTrue(main.contains("commonSetup") || main.contains("FMLCommonSetupEvent"));
        String around = main.substring(Math.max(0, register - 200), Math.min(main.length(), register + 80));
        assertFalse(around.contains("Dist.CLIENT"));
        assertFalse(around.contains("DistExecutor"));
    }

    @Test
    void lifecycleHooksRemainEventDrivenNotHudInferred() throws Exception {
        String drop = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/map/CSMap.java"));
        assertTrue(drop.contains("ItemEntity dropC4") || drop.contains("@Nullable") && drop.contains("dropC4"));
        assertTrue(drop.contains("return dropped") || drop.contains("ItemEntity dropped"));

        String events = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameEvents.java"));
        assertTrue(events.contains("objectiveTracker().manualDrop") || events.contains("manualDrop("));
        assertTrue(events.contains("onPlacedC4") || events.contains("planted("));
        assertFalse(events.contains("BombFuseS2CPacket"));
        assertFalse(events.contains("BOSpecManager"));

        String entity = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/entity/CompositionC4Entity.java"));
        assertTrue(entity.contains("objectiveTracker().defused") || entity.contains(".defused("));
        assertTrue(entity.contains("objectiveTracker().exploded") || entity.contains(".exploded("));
    }

    @Test
    void allGate4ProviderUnitsHaveTests() throws Exception {
        List<String> expectedTests = List.of(
                "CSDMTeamSemanticsTest.java",
                "BlockOffensiveMinimapExtensionTest.java",
                "BlockOffensiveMinimapMountTest.java",
                "CSGameMinimapRegionProviderTest.java",
                "CSMapMinimapMarkerProviderTest.java",
                "CSMinimapIdentityResolverTest.java",
                "CSMinimapVisibilityPolicyTest.java",
                "CSGameObjectiveTrackerTest.java",
                "CSHudSafeAreaLayoutsTest.java",
                "CSMinimapAssetCatalogTest.java"
        );
        Path testDir = Path.of("src/test/java/com/phasetranscrystal/blockoffensive/minimap");
        for (String name : expectedTests) {
            assertTrue(Files.isRegularFile(testDir.resolve(name)), "missing " + name);
        }
    }
}
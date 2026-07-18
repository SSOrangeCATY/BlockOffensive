package com.phasetranscrystal.blockoffensive.minimap;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dedicated-server smoke: common extension initialization sources must not depend on BO client/HUD/render.
 * Full process isolation would require runServer; this guards the classpath surface used by commonSetup.
 */
class CSMinimapDedicatedServerSmokeTest {
    @Test
    void extensionClassIsCommonAndHasNoClientBytecodeReferencesInSource() throws Exception {
        Path ext = Path.of("src/main/java/com/phasetranscrystal/blockoffensive/minimap/BlockOffensiveMinimapExtension.java");
        String src = Files.readString(ext, StandardCharsets.UTF_8);
        assertFalse(src.contains("net.minecraft.client"));
        assertFalse(src.contains("blockoffensive.client"));
        assertFalse(src.contains("GuiGraphics"));
        assertFalse(src.contains("RenderSystem"));
        assertTrue(src.contains("implements MinimapGameplayExtension"));
        assertTrue(src.contains("public static void register()"));
    }

    @Test
    void commonSetupRegistersExtensionWithoutClientBootstrap() throws Exception {
        String main = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/BlockOffensive.java"), StandardCharsets.UTF_8);
        String bootstrap = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/client/BOClientBootstrap.java"), StandardCharsets.UTF_8);
        assertTrue(main.contains("BlockOffensiveMinimapExtension.register()"));
        assertFalse(main.contains("BOClientBootstrap"));
        // client bootstrap may register safe areas, but not the common extension
        assertFalse(bootstrap.contains("BlockOffensiveMinimapExtension"));
    }

    @Test
    void pureTrackersAreFinalServerSafeTypes() {
        assertTrue(Modifier.isFinal(CSGameObjectiveTracker.class.getModifiers()));
        assertTrue(Modifier.isFinal(CSMinimapVisibilityPolicy.class.getModifiers()));
        assertTrue(Modifier.isFinal(CSMapMinimapMarkerProvider.class.getModifiers()));
        assertTrue(Modifier.isFinal(CSGameMinimapRegionProvider.class.getModifiers()));
    }
}
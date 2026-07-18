package com.phasetranscrystal.blockoffensive.minimap;

import com.phasetranscrystal.fpsmatch.common.capability.map.MinimapCapability;
import com.phasetranscrystal.fpsmatch.core.capability.map.MapCapability;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure/source tests only: BO unit classpath does not include Mojang DFU used by Codec fields.
 * Runtime binding activity is covered in FPSMatch MinimapCapabilityTest.
 */
class BlockOffensiveMinimapMountTest {
    @Test
    void csAndCsdmSourceMountMinimapCapability() throws Exception {
        String cs = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameMap.java"));
        String csdm = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/map/CSDeathMatchMap.java"));
        assertTrue(cs.contains("MinimapCapability.class"));
        assertTrue(csdm.contains("MinimapCapability.class"));
        assertTrue(BlockOffensiveMinimapMount.mountsMinimap(
                BlockOffensiveMinimapMount.withMinimap(List.of())
        ));
    }

    @Test
    void withMinimapIsIdempotent() {
        List<Class<? extends MapCapability>> once = BlockOffensiveMinimapMount.withMinimap(new ArrayList<>());
        List<Class<? extends MapCapability>> twice = BlockOffensiveMinimapMount.withMinimap(once);
        assertEquals(1, twice.stream().filter(MinimapCapability.class::equals).count());
    }
}
package com.phasetranscrystal.blockoffensive.minimap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source-guard tests for BO extension registration.
 * Registry behavior is covered in FPSMatch MinimapExtensionRegistryTest.
 */
class BlockOffensiveMinimapExtensionTest {
    @Test
    void extensionSupportsOnlyCsAndCsdmInSource() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/phasetranscrystal/blockoffensive/minimap/BlockOffensiveMinimapExtension.java"
        ));
        assertTrue(source.contains("\"cs\".equals(gameType)"));
        assertTrue(source.contains("\"csdm\".equals(gameType)"));
        assertTrue(source.contains("blockoffensive:cs_csdm"));
    }

    @Test
    void commonSetupSourceRegistersExtension() throws Exception {
        String main = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/BlockOffensive.java"));
        assertTrue(main.contains("BlockOffensiveMinimapExtension.register()"));
    }

    @Test
    void fpsmatchRegistryHasNoGameTypeBranches() throws Exception {
        String core = Files.readString(Path.of(
                "FPSMatch/src/main/java/com/phasetranscrystal/fpsmatch/core/minimap/extension/MinimapExtensionRegistry.java"
        ));
        assertFalse(core.contains("\"cs\""));
        assertFalse(core.contains("\"csdm\""));
        assertFalse(core.contains("blockoffensive"));
    }
}
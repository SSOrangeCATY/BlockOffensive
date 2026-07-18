package com.phasetranscrystal.blockoffensive.minimap;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CSMinimapGameTestResourceTest {
    @Test
    void emptyGameTestStructureIsPackagedUnderBlockOffensiveNamespace() throws Exception {
        Path structure = Path.of(
                "src/main/resources/data/blockoffensive/gameteststructures/empty.snbt"
        );

        assertTrue(Files.isRegularFile(structure), "missing GameTest empty structure");
        String snbt = Files.readString(structure, StandardCharsets.UTF_8);
        assertTrue(snbt.contains("size: [1, 1, 1]"));
        assertTrue(snbt.contains("palette"));
        assertTrue(snbt.contains("blocks"));
        assertTrue(snbt.contains("entities"));
    }

    @Test
    void gameTestRunPreparationCopiesNamespacedSnbtToForgeFallbackDirectory() throws Exception {
        String build = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);

        assertTrue(build.contains("prepareBlockOffensiveGameTestStructures"));
        assertTrue(build.contains("src/main/resources/data/blockoffensive/gameteststructures"));
        assertTrue(build.contains("run/gameteststructures"));
        assertTrue(build.contains("tasks.named('prepareGameTestServerRun')"));
    }
}

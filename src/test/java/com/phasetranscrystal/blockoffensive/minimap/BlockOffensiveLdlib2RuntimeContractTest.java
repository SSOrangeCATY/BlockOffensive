package com.phasetranscrystal.blockoffensive.minimap;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockOffensiveLdlib2RuntimeContractTest {
    @Test
    void blockOffensiveDirectlyDependsOnFpsmatchPinnedLdlib2Artifacts() throws Exception {
        String build = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
        String metadata = Files.readString(
                Path.of("src/main/templates/META-INF/mods.toml"),
                StandardCharsets.UTF_8
        );

        assertTrue(build.contains("FPSMatch/gradle.properties"));
        assertTrue(build.contains("ldlib2_runtime_jar"));
        assertTrue(build.contains("FPSMatch/build/ldlib2"));
        assertTrue(build.contains("name = 'FPSMatchPinnedLdlib2'"));
        assertTrue(build.contains("modImplementation(ldlib2Coordinates)"));
        assertTrue(build.contains("classifier = 'all'"));
        assertTrue(build.contains("gradle.includedBuild('FPSMatch').task(':verifyLdlib2ReleaseArtifacts')"));
        assertTrue(build.contains("'prepareGameTestServerRun'"));
        assertTrue(metadata.contains("modId=\"ldlib2\""));
        assertTrue(metadata.contains("versionRange=\"${ldlib2_version_range}\""));
        assertTrue(!build.contains("modCompileOnly(ldlib2Coordinates)"));
        assertTrue(!build.contains("modRuntimeOnly(ldlib2Coordinates)"));
        assertTrue(!build.contains("jarJar(files(fpsmatchLdlib2RuntimeArtifact))"));
    }

    @Test
    void rootHeadlessClientAcceptanceIncludesFpsmatchAndGameplayAdapters() throws Exception {
        String build = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);

        assertTrue(build.contains("tasks.register('headlessClientAcceptance', Test)"));
        assertTrue(build.contains("gradle.includedBuild('FPSMatch').task(':headlessClientAcceptance')"));
        assertTrue(build.contains("systemProperty 'java.awt.headless', 'true'"));
        assertTrue(build.contains("com.phasetranscrystal.blockoffensive.minimap.*"));
    }
}

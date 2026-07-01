package com.phasetranscrystal.blockoffensive.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KillCamShaderResourceTest {

    @Test
    void effectProgramUsesNamespacedShaderStages() throws IOException {
        String program = Files.readString(Path.of("src/main/resources/assets/blockoffensive/shaders/program/killcam_gray.json"));

        assertTrue(program.contains("\"vertex\": \"blockoffensive:killcam_gray\""));
        assertTrue(program.contains("\"fragment\": \"blockoffensive:killcam_gray\""));
    }

    @Test
    void postChainReferencesNamespacedKillCamProgramAndResourcesExist() throws IOException {
        String postChain = Files.readString(Path.of("src/main/resources/assets/blockoffensive/shaders/post/killcam_gray.json"));

        assertTrue(postChain.contains("\"name\": \"blockoffensive:killcam_gray\""));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/blockoffensive/shaders/program/killcam_gray.json")));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/blockoffensive/shaders/program/killcam_gray.vsh")));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/blockoffensive/shaders/program/killcam_gray.fsh")));
    }
}

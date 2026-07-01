package com.phasetranscrystal.blockoffensive.sound;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MvpMusicManagerSourceGuardTest {
    @Test
    void singletonInstanceIsVolatileForAsyncDataReloadVisibility() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/sound/MVPMusicManager.java"));

        assertTrue(source.contains("private static volatile MVPMusicManager INSTANCE"));
    }
}

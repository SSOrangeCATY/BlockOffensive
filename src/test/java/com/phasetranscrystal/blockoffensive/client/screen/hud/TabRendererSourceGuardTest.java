package com.phasetranscrystal.blockoffensive.client.screen.hud;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabRendererSourceGuardTest {
    private static String readSource(String fileName) throws IOException {
        return Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/client/screen/hud", fileName));
    }

    @Test
    void csTabRendererDoesNotForceUnsyncedPlayerData() throws IOException {
        String source = readSource("CSGameTabRenderer.java");

        assertFalse(source.contains("getPlayerData(uuid).get()"));
        assertTrue(source.contains("getPlayerData(uuid).orElse(null)"));
        assertTrue(source.contains("if (tabData == null)"));
    }

    @Test
    void csTabRendererHandlesMissingTeamBuckets() throws IOException {
        String source = readSource("CSGameTabRenderer.java");

        assertFalse(source.contains("teamPlayers.get(\"ct\")"));
        assertFalse(source.contains("teamPlayers.get(\"t\")"));
        assertTrue(source.contains("teamPlayers.getOrDefault(\"ct\", Collections.emptyList())"));
        assertTrue(source.contains("teamPlayers.getOrDefault(\"t\", Collections.emptyList())"));
    }

    @Test
    void csdmTabRendererDoesNotForceUnsyncedPlayerData() throws IOException {
        String source = readSource("CSDMTabRenderer.java");

        assertFalse(source.contains("getPlayerData(player.getProfile().getId()).get()"));
        assertTrue(source.contains("getPlayerData(player.getProfile().getId()).orElse(null)"));
        assertTrue(source.contains("if (tabData == null)"));
    }
}

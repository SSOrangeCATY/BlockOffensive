package com.phasetranscrystal.blockoffensive.map;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSIssueSourceGuardTest {
    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    @Test
    void gameRoundTimeoutIsOwnedByCSRuleSoPlantedBombCanOvertimeRoundClock() throws IOException {
        String source = read("src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameMap.java");

        assertTrue(source.contains(".roundTicks(Integer.MAX_VALUE)"));
        assertTrue(source.contains(".addRule(new CSRoundTimeoutRule(getRoundTimeLimitTicks()))"));
    }

    @Test
    void oldRoundVictoryMethodsStayRemoved() throws IOException {
        String source = read("src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameMap.java");

        assertFalse(source.contains("checkRoundVictory("));
        assertFalse(source.contains("checkBlastingVictory("));
    }

    @Test
    void livingPlayerAmmoResetDoesNotCallAllPlayersResetInsideLoop() throws IOException {
        String gameMap = read("src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameMap.java");
        String csMap = read("src/main/java/com/phasetranscrystal/blockoffensive/map/CSMap.java");

        assertFalse(gameMap.contains("resetGunAmmo();"));
        assertFalse(csMap.contains("void resetGunAmmo()"));
        assertTrue(gameMap.contains("FPSMUtil.resetAllGunAmmo(player);"));
    }

    @Test
    void pauseCommandHasTopLevelBrigadierAliases() throws IOException {
        String source = read("src/main/java/com/phasetranscrystal/blockoffensive/command/CSCommand.java");

        assertTrue(source.contains("\"pause\", \"p\", \"unpause\", \"up\", \"agree\", \"a\", \"disagree\", \"da\", \"drop\", \"d\""));
        assertTrue(source.contains("dispatcher.register(Commands.literal(command).executes"));
    }

    @Test
    void droppedBombItemsDoNotUseGlobalVanillaGlow() throws IOException {
        String csMap = read("src/main/java/com/phasetranscrystal/blockoffensive/map/CSMap.java");
        String gameMap = read("src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameMap.java");
        String gameEvents = read("src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameEvents.java");

        assertFalse(csMap.contains("setGlowingTag("));
        assertFalse(gameMap.contains("setGlowingTag("));
        assertFalse(gameEvents.contains("setGlowingTag("));
    }

    @Test
    void leavingCsMapUsesConfiguredMatchEndTeleportPoint() throws IOException {
        String csMap = read("src/main/java/com/phasetranscrystal/blockoffensive/map/CSMap.java");

        assertTrue(csMap.contains("public void leave(ServerPlayer player)"));
        assertTrue(csMap.contains("boolean wasInMap = checkGameHasPlayer(player) || checkSpecHasPlayer(player);"));
        assertTrue(csMap.contains("super.leave(player);"));
        assertTrue(csMap.contains("if (wasInMap && !checkGameHasPlayer(player) && !checkSpecHasPlayer(player))"));
        assertTrue(csMap.contains("teleportPlayerToMatchEndPoint(player);"));
    }
}

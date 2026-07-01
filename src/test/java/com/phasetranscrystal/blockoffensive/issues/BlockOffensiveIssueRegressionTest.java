package com.phasetranscrystal.blockoffensive.issues;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockOffensiveIssueRegressionTest {
    private static final Path CS_GAME_MAP = Path.of("src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameMap.java");
    private static final Path CS_MAP = Path.of("src/main/java/com/phasetranscrystal/blockoffensive/map/CSMap.java");
    private static final Path CS_GAME_EVENTS = Path.of("src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameEvents.java");
    private static final Path CS_COMMAND = Path.of("src/main/java/com/phasetranscrystal/blockoffensive/command/CSCommand.java");

    @Test
    void droppedObjectiveItemsDoNotUseVanillaGlowing() throws IOException {
        assertFalse(Files.readString(CS_MAP).contains("setGlowingTag(true)"));
        assertFalse(Files.readString(CS_GAME_MAP).contains("setGlowingTag(true)"));
        assertFalse(Files.readString(CS_GAME_EVENTS).contains("setGlowingTag(true)"));
    }

    @Test
    void topLevelPauseAndVoteCommandsRouteToMapCommands() throws IOException {
        String command = Files.readString(CS_COMMAND);

        assertTrue(command.contains("MAP_COMMANDS"));
        assertTrue(command.contains("\"pause\", \"p\", \"unpause\", \"up\", \"agree\", \"a\", \"disagree\", \"da\", \"drop\", \"d\""));
        assertTrue(command.contains("dispatcher.register(Commands.literal(command).executes"));
        assertTrue(command.contains("csGameMap.handleChatCommand(action,player)"));
    }

    @Test
    void roundResetOnlyRefillsCurrentLivingPlayerAmmo() throws IOException {
        String csGameMap = Files.readString(CS_GAME_MAP);
        String processJoinedPlayer = csGameMap.substring(csGameMap.indexOf("private void processJoinedPlayer"), csGameMap.indexOf("@Override\n    public void givePlayerKits"));

        assertFalse(processJoinedPlayer.contains("resetGunAmmo();"));
        assertTrue(processJoinedPlayer.contains("FPSMUtil.resetAllGunAmmo(player);"));
    }

    @Test
    void plantedBombPreventsRoundClockTimeout() throws IOException {
        String csGameMap = Files.readString(CS_GAME_MAP);
        String isRoundTimeEnd = csGameMap.substring(csGameMap.indexOf("public boolean isRoundTimeEnd"), csGameMap.indexOf("public boolean isClosedShop"));

        assertTrue(isRoundTimeEnd.contains("this.blastState() != BlastBombState.NONE"));
        assertTrue(isRoundTimeEnd.contains("return false;"));
        assertTrue(isRoundTimeEnd.contains("this.currentRoundTime = -1;"));
    }

    @Test
    void legacyVictoryChecksRemainActiveOnOldTickLoop() throws IOException {
        String csGameMap = Files.readString(CS_GAME_MAP);

        assertTrue(csGameMap.contains("case TICKING : this.checkBlastingVictory(); break;"));
        assertTrue(csGameMap.contains("case NONE : if(flag) this.checkRoundVictory(); break;"));
        assertTrue(csGameMap.contains("public void checkRoundVictory()"));
        assertTrue(csGameMap.contains("public void checkBlastingVictory()"));
    }

    @Test
    void csRespawnKeepsClassicModeDeadButLetsDeathmatchRespawn() throws IOException {
        String csMap = Files.readString(CS_MAP);
        String handleRespawn = csMap.substring(csMap.indexOf("public void handleRespawn"), csMap.indexOf("public static void dropC4"));

        assertTrue(handleRespawn.contains("CSDeathMatchMap deathMatchMap"));
        assertTrue(handleRespawn.contains("deathMatchMap.respawnPlayer(player);"));
        assertTrue(handleRespawn.contains("data.setLiving(false)"));
        assertTrue(handleRespawn.contains("player.setGameMode(GameType.SPECTATOR)"));
    }
}

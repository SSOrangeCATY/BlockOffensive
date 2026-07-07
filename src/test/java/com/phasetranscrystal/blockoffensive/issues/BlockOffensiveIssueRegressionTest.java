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
    private static final Path C4_OUTLINE_MIXIN = Path.of("src/main/java/com/phasetranscrystal/blockoffensive/mixin/client/C4ItemOutlineMixin.java");
    private static final Path MIXINS_CONFIG = Path.of("src/main/resources/blockoffensive.mixins.json");

    @Test
    void csTabRendererHandlesMissingTeamsAndPlayerData() throws IOException {
        String tabRenderer = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/client/screen/hud/CSGameTabRenderer.java"));
        String dmRenderer = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/client/screen/hud/CSDMTabRenderer.java"));

        assertTrue(tabRenderer.contains("getOrDefault(\"ct\", Collections.emptyList())"));
        assertTrue(tabRenderer.contains("getOrDefault(\"t\", Collections.emptyList())"));
        assertTrue(tabRenderer.contains("orElse(null)"));
        assertFalse(tabRenderer.contains("getPlayerData(uuid).get()"));

        assertTrue(dmRenderer.contains("map(PlayerData::getScores).orElse(0)"));
        assertTrue(dmRenderer.contains("orElse(null)"));
        assertFalse(dmRenderer.contains("getPlayerData(player.getProfile().getId()).get()"));
    }

    @Test
    void csMvpAwardsDisconnectedPlayersAndPreAwardsFinalKillAssist() throws IOException {
        String csGameMap = Files.readString(CS_GAME_MAP);

        assertTrue(csGameMap.contains("awardRoundMvp(winnerTeam, result.uuid())"));
        assertTrue(csGameMap.contains("winnerTeam.getPlayers().get(uuid)"));
        assertTrue(csGameMap.contains("pendingFinalKillAssist"));
        assertTrue(csGameMap.contains("calculatePendingFinalKillAssist(context)"));
        assertTrue(csGameMap.contains("data.getTempAssists() + pendingFinalKillAssistBonus(data.getOwner())"));
        assertFalse(csGameMap.contains("winnerTeam.getPlayerData(result.uuid()).ifPresent(data -> data.addMvpCount(1))"));
    }

    @Test
    void mvpScorerTestsUseProductionPossibleTypedDamage() throws IOException {
        String scorerTest = Files.readString(Path.of("src/test/java/com/phasetranscrystal/blockoffensive/mvp/CSMvpScorerTest.java"));

        assertFalse(scorerTest.contains("new CSMvpContribution(incendiary, 0, 0, 0.0F"));
        assertFalse(scorerTest.contains("new CSMvpContribution(explosive, 0, 0, 0.0F"));
        assertTrue(scorerTest.contains("80.0F, 0, 0, 0, 80.0F"));
        assertTrue(scorerTest.contains("120.0F, 0, 0, 0, 0.0F, 120.0F"));
    }

    @Test
    void droppedObjectiveItemsDoNotUseVanillaGlowing() throws IOException {
        assertFalse(Files.readString(CS_MAP).contains("setGlowingTag("));
        assertFalse(Files.readString(CS_GAME_MAP).contains("setGlowingTag("));
        assertFalse(Files.readString(CS_GAME_EVENTS).contains("setGlowingTag("));
    }

    @Test
    void droppedC4OutlineIsClientSideAndTTeamOnly() throws IOException {
        String mixin = Files.readString(C4_OUTLINE_MIXIN);
        String mixinsConfig = Files.readString(MIXINS_CONFIG);

        assertTrue(mixin.contains("@OnlyIn(Dist.CLIENT)"));
        assertTrue(mixin.contains("isCurrentGameType(\"cs\")"));
        assertTrue(mixin.contains("isCurrentTeam(\"t\")"));
        assertTrue(mixin.contains("stack.is(BOItemRegister.C4.get())"));
        assertTrue(mixin.contains("shouldRenderAtSqrDistance"));
        assertTrue(mixin.contains("C4_OUTLINE_RENDER_DISTANCE_SQR"));
        assertTrue(mixin.contains("cir.setReturnValue(canLocalPlayerSeeDroppedC4Outline())"));
        assertTrue(mixinsConfig.contains("client.C4ItemOutlineMixin"));
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
        String processJoinedPlayer = csGameMap.substring(
                csGameMap.indexOf("private void processJoinedPlayer"),
                csGameMap.indexOf("public void givePlayerKits")
        );

        assertFalse(processJoinedPlayer.contains("resetGunAmmo();"));
        assertTrue(processJoinedPlayer.contains("FPSMUtil.resetAllGunAmmo(player);"));
    }

    @Test
    void roundTimeoutDoesNotEndRoundWhileBombIsPlanted() throws IOException {
        String csGameMap = Files.readString(CS_GAME_MAP);
        String lifecycle = csGameMap.substring(
                csGameMap.indexOf("protected RoundLifecycle"),
                csGameMap.indexOf("protected RoundContext createRoundContext")
        );

        assertTrue(lifecycle.contains(".roundTicks(Integer.MAX_VALUE)"));
        assertTrue(lifecycle.contains(".addRule(new CSRoundTimeoutRule(getRoundTimeLimitTicks()))"));
        assertFalse(lifecycle.contains("timeoutResult(() -> new RoundResult"));
    }

    @Test
    void obsoleteVictoryChecksAreRemoved() throws IOException {
        String csGameMap = Files.readString(CS_GAME_MAP);

        assertFalse(csGameMap.contains("public void checkRoundVictory()"));
        assertFalse(csGameMap.contains("public void checkBlastingVictory()"));
        assertFalse(csGameMap.contains("@see #checkRoundVictory()"));
        assertFalse(csGameMap.contains("@see #checkBlastingVictory()"));
    }

    @Test
    void csRespawnKeepsClassicModeDeadButLetsDeathmatchRespawn() throws IOException {
        String csMap = Files.readString(CS_MAP);
        String handleRespawn = csMap.substring(csMap.indexOf("public void handleRespawn"), csMap.indexOf("public static void dropC4"));

        assertTrue(csMap.contains("extends BaseRoundMap<String, CSRoundResultReason>"));
        assertTrue(handleRespawn.contains("CSDeathMatchMap deathMatchMap"));
        assertTrue(handleRespawn.contains("deathMatchMap.respawnPlayer(player);"));
        assertTrue(handleRespawn.contains("data.setLiving(false)"));
        assertTrue(handleRespawn.contains("player.setGameMode(GameType.SPECTATOR)"));
    }

    @Test
    void classicModeSendsPhysicsDeathBeforeSpectatorStateChange() throws IOException {
        String csGameMap = Files.readString(CS_GAME_MAP);
        String handleDeath = csGameMap.substring(
                csGameMap.indexOf("public void handleDeath(DeathContext context)"),
                csGameMap.indexOf("private UUID calculatePendingFinalKillAssist")
        );
        int sendPhysicsDeath = handleDeath.indexOf("sendPhysicsDeathPacket(dead);");
        int setSpectator = handleDeath.indexOf("dead.setGameMode(GameType.SPECTATOR);");

        assertTrue(Files.readString(CS_MAP).contains("protected void sendPhysicsDeathPacket(ServerPlayer deadPlayer)"));
        assertTrue(Files.readString(CS_MAP).contains("protected boolean shouldSendPhysicsDeathPacketInBaseDeathHandler(DeathContext context)"));
        assertTrue(handleDeath.contains("super.handleDeath(context);"));
        assertTrue(csGameMap.contains("protected boolean shouldSendPhysicsDeathPacketInBaseDeathHandler(DeathContext context)"));
        assertTrue(csGameMap.contains("return !this.isStart || this.getMapTeams().getTeamByPlayer(context.getDeadPlayer()).isEmpty();"));
        assertTrue(sendPhysicsDeath >= 0);
        assertTrue(setSpectator >= 0);
        assertTrue(sendPhysicsDeath < setSpectator);
    }
}

package com.phasetranscrystal.blockoffensive.issues;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockOffensiveIssueRegressionTest {

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
        String csGameMap = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/map/CSGameMap.java"));

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
}

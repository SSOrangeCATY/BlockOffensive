package com.phasetranscrystal.blockoffensive.mvp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CSMvpScorerTest {
    @Test
    void selectsDamageMvpWhenCombatScoresTie() {
        UUID support = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID fragger = UUID.fromString("00000000-0000-0000-0000-000000000002");

        CSMvpResult result = CSMvpScorer.selectRoundMvp(List.of(
                contrib(support, 1, 1, 120.0F, 0, 0, 0, false, false, false),
                contrib(fragger, 1, 1, 80.0F, 0, 0, 0, false, false, false)
        ));

        assertEquals(support, result.uuid());
        assertEquals("blockoffensive.mvp.high_damage", result.reasonKey());
        assertEquals("blockoffensive.mvp.info.high_damage", result.infoKey());
    }

    @Test
    void selectsDefuserOverCombatContribution() {
        UUID defuser = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID fragger = UUID.fromString("00000000-0000-0000-0000-000000000004");

        CSMvpResult result = CSMvpScorer.selectRoundMvp(List.of(
                contrib(defuser, 0, 0, 0.0F, 0, 0, 0, true, false, false),
                contrib(fragger, 2, 0, 120.0F, 0, 0, 0, false, false, false)
        ));

        assertEquals(defuser, result.uuid());
        assertEquals("blockoffensive.mvp.defuse", result.reasonKey());
        assertEquals("blockoffensive.mvp.info.defuse", result.infoKey());
    }

    @Test
    void prefersIncendiaryDamageOverExplosiveDamageWhenScoresTie() {
        UUID incendiary = UUID.fromString("00000000-0000-0000-0000-000000000005");
        UUID explosive = UUID.fromString("00000000-0000-0000-0000-000000000006");

        CSMvpResult result = CSMvpScorer.selectRoundMvp(List.of(
                contrib(incendiary, 0, 0, 80.0F, 0, 80.0F, 0, false, false, false),
                contrib(explosive, 0, 0, 120.0F, 0, 0, 120.0F, false, false, false)
        ));

        assertEquals(incendiary, result.uuid());
        assertEquals("blockoffensive.mvp.incendiary_damage", result.reasonKey());
        assertEquals("blockoffensive.mvp.info.incendiary_damage", result.infoKey());
    }

    @Test
    void doesNotDoubleCountIncendiaryDamageAlreadyIncludedInTotalDamage() {
        UUID bullet = UUID.fromString("00000000-0000-0000-0000-000000000007");
        UUID incendiary = UUID.fromString("00000000-0000-0000-0000-000000000008");

        CSMvpResult result = CSMvpScorer.selectRoundMvp(List.of(
                contrib(bullet, 0, 0, 250.0F, 0, 0, 0, false, false, false),
                contrib(incendiary, 0, 0, 100.0F, 0, 100.0F, 0, false, false, false)
        ));

        assertEquals(bullet, result.uuid());
    }

    @Test
    void headshotsBreakCombatScoreTie() {
        UUID headshot = UUID.fromString("00000000-0000-0000-0000-000000000009");
        UUID bodyShot = UUID.fromString("00000000-0000-0000-0000-000000000010");

        CSMvpResult result = CSMvpScorer.selectRoundMvp(List.of(
                contrib(headshot, 1, 0, 50.0F, 1, 0, 0, false, false, false),
                contrib(bodyShot, 1, 0, 50.0F, 0, 0, 0, false, false, false)
        ));

        assertEquals(headshot, result.uuid());
    }

    @Test
    void doesNotLabelUtilityReasonWhenCombatDominates() {
        UUID fragger = UUID.fromString("00000000-0000-0000-0000-000000000011");

        CSMvpResult result = CSMvpScorer.selectRoundMvp(List.of(
                contrib(fragger, 2, 0, 200.0F, 1, 10.0F, 0, false, false, false)
        ));

        assertEquals(fragger, result.uuid());
        assertEquals("blockoffensive.mvp.high_damage", result.reasonKey());
        assertEquals("blockoffensive.mvp.info.high_damage", result.infoKey());
    }

    @Test
    void labelsExplosiveDamageWhenUtilityDominates() {
        UUID nader = UUID.fromString("00000000-0000-0000-0000-000000000012");

        CSMvpResult result = CSMvpScorer.selectRoundMvp(List.of(
                contrib(nader, 0, 0, 180.0F, 0, 0, 180.0F, false, false, false)
        ));

        assertEquals(nader, result.uuid());
        assertEquals("blockoffensive.mvp.explosive_damage", result.reasonKey());
        assertEquals("blockoffensive.mvp.info.explosive_damage", result.infoKey());
    }

    @Test
    void clutchSurviveBeatsPureDamageAndUsesSurviveReason() {
        UUID clutch = UUID.fromString("00000000-0000-0000-0000-000000000013");
        UUID fragger = UUID.fromString("00000000-0000-0000-0000-000000000014");

        CSMvpResult result = CSMvpScorer.selectRoundMvp(List.of(
                contrib(clutch, 1, 0, 40.0F, 0, 0, 0, false, false, true),
                contrib(fragger, 2, 0, 180.0F, 0, 0, 0, false, false, false)
        ));

        assertEquals(clutch, result.uuid());
        assertEquals("blockoffensive.mvp.survive_under_fire", result.reasonKey());
        assertEquals("blockoffensive.mvp.info.survive_under_fire", result.infoKey());
    }

    @Test
    void defuseStillBeatsClutchSurvive() {
        UUID defuser = UUID.fromString("00000000-0000-0000-0000-000000000015");
        UUID clutch = UUID.fromString("00000000-0000-0000-0000-000000000016");

        CSMvpResult result = CSMvpScorer.selectRoundMvp(List.of(
                contrib(defuser, 0, 0, 0.0F, 0, 0, 0, true, false, false),
                contrib(clutch, 3, 0, 200.0F, 1, 0, 0, false, false, true)
        ));

        assertEquals(defuser, result.uuid());
        assertEquals("blockoffensive.mvp.defuse", result.reasonKey());
    }

    private static CSMvpContribution contrib(
            UUID uuid,
            int kills,
            int assists,
            float damage,
            int headshots,
            float fire,
            float explosive,
            boolean defuse,
            boolean plant,
            boolean clutch
    ) {
        return new CSMvpContribution(
                uuid, kills, assists, damage, headshots, 0, 0, fire, explosive, defuse, plant, clutch
        );
    }
}

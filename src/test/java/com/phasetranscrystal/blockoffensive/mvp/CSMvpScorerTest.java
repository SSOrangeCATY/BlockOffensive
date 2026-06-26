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
                new CSMvpContribution(support, 1, 1, 120.0F, 0, 0, 0, 0.0F, 0.0F, false, false),
                new CSMvpContribution(fragger, 1, 1, 80.0F, 0, 0, 0, 0.0F, 0.0F, false, false)
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
                new CSMvpContribution(defuser, 0, 0, 0.0F, 0, 0, 0, 0.0F, 0.0F, true, false),
                new CSMvpContribution(fragger, 2, 0, 120.0F, 0, 0, 0, 0.0F, 0.0F, false, false)
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
                new CSMvpContribution(incendiary, 0, 0, 0.0F, 0, 0, 0, 80.0F, 0.0F, false, false),
                new CSMvpContribution(explosive, 0, 0, 0.0F, 0, 0, 0, 0.0F, 120.0F, false, false)
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
                new CSMvpContribution(bullet, 0, 0, 250.0F, 0, 0, 0, 0.0F, 0.0F, false, false),
                new CSMvpContribution(incendiary, 0, 0, 100.0F, 0, 0, 0, 100.0F, 0.0F, false, false)
        ));

        assertEquals(bullet, result.uuid());
    }

    @Test
    void headshotsBreakCombatScoreTie() {
        UUID headshot = UUID.fromString("00000000-0000-0000-0000-000000000009");
        UUID bodyShot = UUID.fromString("00000000-0000-0000-0000-000000000010");

        CSMvpResult result = CSMvpScorer.selectRoundMvp(List.of(
                new CSMvpContribution(headshot, 1, 0, 50.0F, 1, 0, 0, 0.0F, 0.0F, false, false),
                new CSMvpContribution(bodyShot, 1, 0, 50.0F, 0, 0, 0, 0.0F, 0.0F, false, false)
        ));

        assertEquals(headshot, result.uuid());
    }
}

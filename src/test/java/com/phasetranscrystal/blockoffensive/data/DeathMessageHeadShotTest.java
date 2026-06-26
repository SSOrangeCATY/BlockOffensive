package com.phasetranscrystal.blockoffensive.data;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathMessageHeadShotTest {
    static boolean resolveHeadShotFlag(boolean isHeadShot) {
        return isHeadShot;
    }

    static boolean resolveHeadShotFlagBeforeFix(boolean isHeadShot, boolean attackerIsDeadPlayer) {
        boolean resultIsHeadShot = false;
        if (!attackerIsDeadPlayer) {
            resultIsHeadShot = isHeadShot;
        }
        return resultIsHeadShot;
    }

    @Test
    void headShotPreservedWhenAttackerIsRealKiller() {
        assertTrue(resolveHeadShotFlag(true));
        assertFalse(resolveHeadShotFlag(false));
    }

    @Test
    void headShotPreservedWhenAttackerFallbackToDeadPlayer() {
        assertFalse(resolveHeadShotFlagBeforeFix(true, true));
        assertTrue(resolveHeadShotFlag(true));
    }

    @Test
    void scopedKillFlagBecomesNoScopeFlagForRealKill() {
        assertTrue(DeathMessageRules.resolveNoScopeFlag(true, false));
        assertFalse(DeathMessageRules.resolveNoScopeFlag(false, false));
    }

    @Test
    void scopedKillFlagDoesNotMarkSuicideAsNoScope() {
        assertFalse(DeathMessageRules.resolveNoScopeFlag(true, true));
    }

    @Test
    void missingAssistIsNotRenderedAndDoesNotThrow() {
        UUID killer = UUID.fromString("00000000-0000-0000-0000-000000000011");

        assertFalse(DeathMessageRules.hasDistinctAssist(null, killer));
    }

    @Test
    void c4WeaponDoesNotMakeKillMessageASuicideByItself() {
        UUID killer = UUID.fromString("00000000-0000-0000-0000-000000000012");
        UUID dead = UUID.fromString("00000000-0000-0000-0000-000000000013");

        assertFalse(DeathMessageRules.isSuicide(dead, killer));
    }

    @Test
    void sameDeadAndKillerIsStillSuicide() {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000014");

        assertTrue(DeathMessageRules.isSuicide(player, player));
    }
}

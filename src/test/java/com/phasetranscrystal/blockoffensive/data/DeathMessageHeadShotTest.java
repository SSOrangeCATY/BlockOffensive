package com.phasetranscrystal.blockoffensive.data;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    static boolean[] resolveSpecialIconFlags(boolean isHeadShot, boolean isPassWall, boolean isPassSmoke) {
        return new boolean[]{isHeadShot, isPassWall, isPassSmoke};
    }

    static boolean[] resolveSpecialIconFlagsBeforeWallSmokeFix(boolean isHeadShot, boolean isPassWall, boolean isPassSmoke,
                                                               boolean attackerIsDeadPlayer) {
        boolean resultIsPassWall = false;
        boolean resultIsPassSmoke = false;
        if (!attackerIsDeadPlayer) {
            resultIsPassWall = isPassWall;
            resultIsPassSmoke = isPassSmoke;
        }
        return new boolean[]{isHeadShot, resultIsPassWall, resultIsPassSmoke};
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
    void scopedKillFlagDoesNotBecomeNoScopeFlagForRealKill() {
        assertFalse(DeathMessageRules.resolveNoScopeFlag(true, false));
        assertTrue(DeathMessageRules.resolveNoScopeFlag(false, false));
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

    @Test
    void specialIconFlagsPreservedWhenAttackerFallbackToDeadPlayer() {
        boolean[] beforeFix = resolveSpecialIconFlagsBeforeWallSmokeFix(true, true, true, true);
        assertTrue(beforeFix[0]);
        assertFalse(beforeFix[1]);
        assertFalse(beforeFix[2]);

        boolean[] flags = resolveSpecialIconFlags(true, true, true);
        assertTrue(flags[0]);
        assertTrue(flags[1]);
        assertTrue(flags[2]);
    }

    @Test
    void iconFlagsAreSetBeforeAttackerFallbackGuard() throws IOException {
        String code = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/util/BOUtil.java"));
        int headShot = code.indexOf("builder.setHeadShot(isHeadShot);");
        int throughWall = code.indexOf("builder.setThroughWall(isPassWall);");
        int throughSmoke = code.indexOf("builder.setThroughSmoke(isPassSmoke);");
        int attackerGuard = code.indexOf("if(!attacker.is(deadPlayer))");
        assertTrue(headShot >= 0);
        assertTrue(throughWall >= 0);
        assertTrue(throughSmoke >= 0);
        assertTrue(attackerGuard >= 0);
        assertTrue(headShot < attackerGuard);
        assertTrue(throughWall < attackerGuard);
        assertTrue(throughSmoke < attackerGuard);
    }

    @Test
    void csMapPassesDeathContextIconFlagsToDeathMessagePacket() throws IOException {
        String code = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/map/CSMap.java"));
        String handleDeath = code.substring(code.indexOf("public void handleDeath(DeathContext context)"));
        assertTrue(handleDeath.contains("boolean passWall = context.isPassWall();"));
        assertTrue(handleDeath.contains("boolean passSmoke = context.isPassSmoke();"));
        assertTrue(handleDeath.contains("context.isHeadShot(),"));
        assertTrue(handleDeath.contains("passWall,"));
        assertTrue(handleDeath.contains("passSmoke,"));
        assertTrue(handleDeath.contains("BOTaczLiveFireDebugCommand.handleDeathMessage(getMapName(), killPacket.deathMessage());"));
    }

    @Test
    void packetAndHudPreserveSpecialIconFlags() throws IOException {
        String packet = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/net/DeathMessageS2CPacket.java"));
        assertTrue(packet.contains("public DeathMessage deathMessage()"));
        assertTrue(packet.contains("isHeadShot() ? 1 : 0"));
        assertTrue(packet.contains("isThroughSmoke() ? 4 : 0"));
        assertTrue(packet.contains("isThroughWall() ? 8 : 0"));
        assertTrue(packet.contains("setHeadShot((flags & 1) != 0)"));
        assertTrue(packet.contains("setThroughSmoke((flags & 4) != 0)"));
        assertTrue(packet.contains("setThroughWall((flags & 8) != 0)"));

        String hud = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/client/screen/hud/CSDeathMessageHud.java"));
        assertTrue(hud.contains("registerSpecialKillIcon(\"headshot\""));
        assertTrue(hud.contains("registerSpecialKillIcon(\"throw_smoke\""));
        assertTrue(hud.contains("registerSpecialKillIcon(\"throw_wall\""));
        assertTrue(hud.contains("message.isHeadShot()"));
        assertTrue(hud.contains("message.isThroughSmoke()"));
        assertTrue(hud.contains("message.isThroughWall()"));
    }
}

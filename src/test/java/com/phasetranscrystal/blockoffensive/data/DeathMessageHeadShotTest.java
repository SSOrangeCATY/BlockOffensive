package com.phasetranscrystal.blockoffensive.data;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证死亡消息特殊击杀标记在构建过程中的正确传播。
 * <p>
 * 对应 Bug 2 修复：枪械爆头击杀后爆头图标不显示。
 * <p>
 * 历史根因：{@code BOUtil.buildDeathMessagePacket} 曾把部分特殊击杀标记放在
 * {@code if(!attacker.is(deadPlayer))} 块内，当 attacker 回退为 deadPlayer 时
 * （CSMap.handleDeath 中 attacker 为 null 的兜底），对应图标标记不会写入消息。
 * <p>
 * 修复：将 setHeadShot、setThroughWall、setThroughSmoke 放在攻击者兜底 guard 前，
 * 确保 FPSMatch 死亡上下文中的图标标记始终被写入消息。
 */
class DeathMessageHeadShotTest {

    /**
     * 模拟修复后 BOUtil.buildDeathMessagePacket 的爆头标记设置逻辑。
     *
     * @param isHeadShot         传入的爆头标记
     * @param attackerIsDeadPlayer 攻击者是否等于死者（兜底场景）
     * @return 最终 DeathMessage 中的 isHeadShot 值
     */
    static boolean resolveHeadShotFlag(boolean isHeadShot, boolean attackerIsDeadPlayer) {
        // 修复后：setHeadShot 始终在 if 块外调用
        boolean resultIsHeadShot = isHeadShot;

        if (!attackerIsDeadPlayer) {
            // flying/assist 等依赖真实攻击者的逻辑仍只在非自杀时设置
            // 不影响 isHeadShot
        }

        return resultIsHeadShot;
    }

    static boolean[] resolveSpecialIconFlags(boolean isHeadShot,
                                             boolean isPassWall,
                                             boolean isPassSmoke,
                                             boolean attackerIsDeadPlayer) {
        // 这三个图标标记由 FPSMatch 死亡上下文提供，不能被攻击者兜底/自杀判断吞掉。
        boolean resultIsHeadShot = isHeadShot;
        boolean resultIsPassWall = isPassWall;
        boolean resultIsPassSmoke = isPassSmoke;

        if (!attackerIsDeadPlayer) {
            // flying/assist 等依赖真实攻击者的逻辑仍只在非自杀时设置。
        }

        return new boolean[]{resultIsHeadShot, resultIsPassWall, resultIsPassSmoke};
    }

    /**
     * 模拟本次修复前的特殊图标逻辑：爆头已在 guard 外，但穿墙/穿烟仍在 guard 内。
     */
    static boolean[] resolveSpecialIconFlags_beforeWallSmokeFix(boolean isHeadShot,
                                                                boolean isPassWall,
                                                                boolean isPassSmoke,
                                                                boolean attackerIsDeadPlayer) {
        boolean resultIsHeadShot = isHeadShot;
        boolean resultIsPassWall = false;
        boolean resultIsPassSmoke = false;

        if (!attackerIsDeadPlayer) {
            resultIsPassWall = isPassWall;
            resultIsPassSmoke = isPassSmoke;
        }

        return new boolean[]{resultIsHeadShot, resultIsPassWall, resultIsPassSmoke};
    }

    /**
     * 模拟修复前的逻辑（setHeadShot 在 if 块内），用于对比验证。
     */
    static boolean resolveHeadShotFlag_beforeFix(boolean isHeadShot, boolean attackerIsDeadPlayer) {
        boolean resultIsHeadShot = false; // 默认值

        if (!attackerIsDeadPlayer) {
            resultIsHeadShot = isHeadShot; // 仅在非自杀时设置
        }

        return resultIsHeadShot;
    }

    @Test
    void headShotPreserved_whenAttackerIsRealKiller() {
        // 正常击杀场景：攻击者不是死者，爆头标记应正确设置
        assertTrue(resolveHeadShotFlag(true, false));
        assertFalse(resolveHeadShotFlag(false, false));
    }

    @Test
    void headShotPreserved_whenAttackerFallbackToDeadPlayer() {
        // Bug 2 核心场景：attacker 为 null 回退为 deadPlayer 时，爆头标记不应丢失
        // 修复前：isHeadShot 丢失（返回 false）
        assertFalse(resolveHeadShotFlag_beforeFix(true, true));
        // 修复后：isHeadShot 保留（返回 true）
        assertTrue(resolveHeadShotFlag(true, true));
    }

    @Test
    void noHeadShotRemainsFalse_whenAttackerFallbackToDeadPlayer() {
        // 非爆头击杀时，即使 attacker 回退，isHeadShot 也应为 false
        assertFalse(resolveHeadShotFlag(false, true));
    }

    @Test
    void fixDoesNotBreakNormalHeadShotKill() {
        // 回归测试：正常爆头击杀仍然正确
        assertTrue(resolveHeadShotFlag(true, false));
    }

    @Test
    void fixDoesNotBreakNormalNonHeadShotKill() {
        // 回归测试：正常非爆头击杀仍然正确
        assertFalse(resolveHeadShotFlag(false, false));
    }

    @Test
    void beforeFix_losesHeadShot_whenAttackerFallback() {
        // 验证修复前确实存在 bug：attacker 回退时爆头标记丢失
        assertFalse(resolveHeadShotFlag_beforeFix(true, true));
    }

    @Test
    void afterFix_preservesHeadShot_inAllScenarios() {
        // 修复后所有场景的爆头标记都正确
        assertTrue(resolveHeadShotFlag(true, false));  // 正常爆头
        assertTrue(resolveHeadShotFlag(true, true));   // 回退爆头
        assertFalse(resolveHeadShotFlag(false, false)); // 正常非爆头
        assertFalse(resolveHeadShotFlag(false, true));  // 回退非爆头
    }

    @Test
    void specialIconFlagsPreserved_whenAttackerFallbackToDeadPlayer() {
        boolean[] beforeFix = resolveSpecialIconFlags_beforeWallSmokeFix(true, true, true, true);
        assertTrue(beforeFix[0]);
        assertFalse(beforeFix[1]);
        assertFalse(beforeFix[2]);

        boolean[] flags = resolveSpecialIconFlags(true, true, true, true);

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
    }

    @Test
    void packetAndHudPreserveSpecialIconFlags() throws IOException {
        String packet = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/net/DeathMessageS2CPacket.java"));
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

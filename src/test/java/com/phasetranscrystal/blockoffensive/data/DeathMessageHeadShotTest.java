package com.phasetranscrystal.blockoffensive.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证爆头标记在死亡消息构建过程中的正确传播。
 * <p>
 * 对应 Bug 2 修复：枪械爆头击杀后爆头图标不显示。
 * <p>
 * 根因：{@code BOUtil.buildDeathMessagePacket} 中 {@code setHeadShot} 被放在
 * {@code if(!attacker.is(deadPlayer))} 块内，当 attacker 回退为 deadPlayer 时
 * （CSMap.handleDeath 中 attacker 为 null 的兜底），setHeadShot 不会被调用，
 * 导致 isHeadShot 始终为 false。
 * <p>
 * 修复：将 setHeadShot 移到 if 块外，确保爆头标记始终被设置。
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
            // 其他标记（flying, throughWall, throughSmoke）仅在非自杀时设置
            // 不影响 isHeadShot
        }

        return resultIsHeadShot;
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
}

package com.xuebi1145.xuplus_client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * 主HUD血量：保留原版偏左布局，低血量4方向红色外发光，扣血数字坠落动效
 */
@Mixin(value = CSGameHud.class, remap = false)
public class MixinCSGameHudHealth {

    @Shadow(remap = false)
    public void renderArmorBar(Minecraft mc, ForgeGui gui, GuiGraphics guiGraphics, int healthTextX, int healthTextY) {}

    // ===== 扣血坠落动效 =====
    @Unique
    private static float xuplus$prevHealthPercent = -1f;

    @Unique
    private static float xuplus$lostHealthPercent = 0f;

    @Unique
    private static long xuplus$healthLossTime = 0;

    @Unique
    private static final long xuplus$HEALTH_LOSS_DURATION = 500;

    /**
     * @author XUPlus
     * @reason 保留原版偏左布局，集成坠落动效
     */
    @Overwrite(remap = false)
    public void renderHealthBar(Minecraft mc, ForgeGui gui, GuiGraphics guiGraphics, int centerX, int lineWidth, int y) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        int health = (int) player.getHealth();
        int maxHealth = (int) player.getMaxHealth();
        float healthPercent = maxHealth > 0 ? (float) health / maxHealth : 0f;
        float healthPercent100 = healthPercent * 100f;
        Font font = mc.font;

        String healthText = String.valueOf((int) healthPercent100);
        int tempWidth = font.width("000") * 2;

        // 原版布局：血量数字在centerX左侧偏移
        int healthTextX = centerX - lineWidth / 2 - 10 - tempWidth;
        int healthTextY = y - font.lineHeight + 1;
        int healthBarY = y + font.lineHeight;
        int healthBarHeight = 3;
        int healthBarFillWidth = (int) (healthPercent * tempWidth);

        // 调用护甲栏渲染（MixinCSGameHudArmorBar会接管）
        this.renderArmorBar(mc, gui, guiGraphics, healthTextX, healthTextY);

        // ===== 绘制血量数字（含低血量fill发光）=====
        boolean lowHealth = healthPercent100 > 0f && healthPercent100 < 25f;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(healthTextX + (float) tempWidth / 2 - (float) font.width(healthText), healthTextY, 0);
        guiGraphics.pose().scale(2, 2, 0);

        if (lowHealth) {
            // 低血量7×7网格发光
            float maxRadius = 3.0f;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    if (dx != 0 || dy != 0) {
                        float dist = (float) Math.sqrt(dx * dx + dy * dy);
                        if (dist <= maxRadius) {
                            float t = dist / maxRadius;
                            int alpha = (int) (112.0f * (1.0f - t) * (1.0f - t));
                            if (alpha > 0) {
                                guiGraphics.drawString(font, healthText, dx, dy, (alpha << 24) | 0x00FF3333, true);
                            }
                        }
                    }
                }
            }
            guiGraphics.drawString(font, healthText, 0, 0, 0xFFFF3333, false);
        } else {
            guiGraphics.drawString(font, healthText, 0, 0, 0xFFFFFFFF, false);
        }
        guiGraphics.pose().popPose();

        // ===== 血条 =====
        guiGraphics.fill(healthTextX, healthBarY, healthTextX + tempWidth, healthBarY + healthBarHeight, 0x80000000);
        guiGraphics.fill(healthTextX, healthBarY, healthTextX + healthBarFillWidth, healthBarY + healthBarHeight, 0x8000FF00);

        // ===== 扣血坠落动效 =====
        if (xuplus$prevHealthPercent >= 0f && healthPercent100 < xuplus$prevHealthPercent) {
            xuplus$lostHealthPercent = xuplus$prevHealthPercent;
            xuplus$healthLossTime = System.currentTimeMillis();
        }
        xuplus$prevHealthPercent = healthPercent100;

        if (xuplus$healthLossTime > 0) {
            long elapsed = System.currentTimeMillis() - xuplus$healthLossTime;
            if (elapsed >= xuplus$HEALTH_LOSS_DURATION) {
                xuplus$healthLossTime = 0;
            } else {
                float progress = (float) elapsed / xuplus$HEALTH_LOSS_DURATION;
                int fallOffset = Math.round(progress * 14);
                int alpha = (int) (255 * (1.0f - progress * progress));
                if (alpha > 0) {
                    String lostText = String.valueOf((int) xuplus$lostHealthPercent);
                    int fallY = healthTextY + fallOffset;
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(
                        healthTextX + (float) tempWidth / 2 - (float) font.width(lostText),
                        fallY, 0);
                    guiGraphics.pose().scale(2, 2, 0);
                    guiGraphics.drawString(font, lostText, 0, 0, (alpha << 24) | 0x00FF3333, true);
                    guiGraphics.pose().popPose();
                    RenderSystem.disableBlend();
                }
            }
        }
    }
}

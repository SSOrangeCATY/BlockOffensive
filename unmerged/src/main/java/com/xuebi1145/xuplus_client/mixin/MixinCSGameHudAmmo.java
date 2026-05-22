package com.xuebi1145.xuplus_client.mixin;

import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameHud;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修改CSGameHud.renderGunInfo：
 * 弹药量不足25%时7×7网格红色外发光
 * 射击时触发衰减圆形抖动（X=sin, Y=cos，Y振幅=X的0.6倍）
 */
@Mixin(value = CSGameHud.class)
public class MixinCSGameHudAmmo {

    @Unique
    private static float xuplus$cachedAmmoRatio = 1f;

    // ===== 抖动动效状态 =====
    @Unique
    private static int xuplus$prevAmmoCount = -1;

    @Unique
    private static long xuplus$shakeStartTime = 0;

    @Unique
    private static boolean xuplus$shaking = false;

    // 抖动持续时间（毫秒）
    @Unique
    private static final long xuplus$SHAKE_DURATION = 150;

    // 抖动最大振幅（像素，在scale=2的坐标系内，轻微象征性）
    @Unique
    private static final float xuplus$SHAKE_AMPLITUDE = 1.5f;

    @Inject(method = "renderGunInfo", at = @At("HEAD"), remap = false)
    private void xuplus$captureAmmoRatio(Minecraft mc, ForgeGui gui, GuiGraphics guiGraphics, int screenWidth, int screenHeight, ItemStack stack, IGun iGun, int centerX, int lineWidth, int y, CallbackInfo ci) {
        GunData gunData = TimelessAPI.getClientGunIndex(iGun.getGunId(stack)).map(ClientGunIndex::getGunData).orElse(null);
        if (gunData != null) {
            int maxAmmo = AttachmentDataUtils.getAmmoCountWithAttachment(stack, gunData);
            int ammo = iGun.getCurrentAmmoCount(stack) + (iGun.hasBulletInBarrel(stack) && gunData.getBolt() != Bolt.OPEN_BOLT ? 1 : 0);
            xuplus$cachedAmmoRatio = maxAmmo > 0 ? (float) ammo / maxAmmo : 1f;

            // 检测弹量减少 → 触发抖动
            if (xuplus$prevAmmoCount >= 0 && ammo < xuplus$prevAmmoCount) {
                xuplus$shakeStartTime = System.currentTimeMillis();
                xuplus$shaking = true;
            }
            xuplus$prevAmmoCount = ammo;
        }
    }

    /**
     * 计算当前X轴抖动偏移量
     */
    @Unique
    private static float xuplus$getShakeOffsetX() {
        if (!xuplus$shaking) return 0f;
        long elapsed = System.currentTimeMillis() - xuplus$shakeStartTime;
        if (elapsed >= xuplus$SHAKE_DURATION) {
            xuplus$shaking = false;
            return 0f;
        }
        float progress = (float) elapsed / xuplus$SHAKE_DURATION;
        float decay = (1.0f - progress) * (1.0f - progress);
        float wave = (float) Math.sin(progress * Math.PI * 6);
        return xuplus$SHAKE_AMPLITUDE * decay * wave;
    }

    /**
     * 计算当前Y轴抖动偏移量（与X轴相位错开，形成叠加抖动）
     */
    @Unique
    private static float xuplus$getShakeOffsetY() {
        if (!xuplus$shaking) return 0f;
        long elapsed = System.currentTimeMillis() - xuplus$shakeStartTime;
        if (elapsed >= xuplus$SHAKE_DURATION) {
            return 0f;
        }
        float progress = (float) elapsed / xuplus$SHAKE_DURATION;
        float decay = (1.0f - progress) * (1.0f - progress);
        float wave = (float) Math.cos(progress * Math.PI * 6); // cos与sin相位错开90°
        return xuplus$SHAKE_AMPLITUDE * 0.6f * decay * wave; // Y轴振幅稍小
    }

    @Redirect(
        method = "renderGunInfo",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", ordinal = 0)
    )
    private int xuplus$glowAmmoText(GuiGraphics gg, Font font, String text, int x, int y, int color, boolean shadow) {
        // 抖动偏移
        int drawX = x + Math.round(xuplus$getShakeOffsetX());
        int drawY = y + Math.round(xuplus$getShakeOffsetY());

        if (xuplus$cachedAmmoRatio < 0.25f && xuplus$cachedAmmoRatio > 0f) {
            // 7×7网格发光
            float maxRadius = 3.0f;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    if (dx != 0 || dy != 0) {
                        float dist = (float) Math.sqrt(dx * dx + dy * dy);
                        if (dist <= maxRadius) {
                            float t = dist / maxRadius;
                            int alpha = (int) (112.0f * (1.0f - t) * (1.0f - t));
                            if (alpha > 0) {
                                int glowColor = (alpha << 24) | 0x00FF3333;
                                gg.drawString(font, text, drawX + dx, drawY + dy, glowColor, true);
                            }
                        }
                    }
                }
            }
            return gg.drawString(font, text, drawX, drawY, 0xFFFF3333, false);
        }
        // 非低弹量时也应用抖动（射击瞬间）
        if (drawX != x || drawY != y) {
            return gg.drawString(font, text, drawX, drawY, color, shadow);
        }
        return gg.drawString(font, text, x, y, color, shadow);
    }
}

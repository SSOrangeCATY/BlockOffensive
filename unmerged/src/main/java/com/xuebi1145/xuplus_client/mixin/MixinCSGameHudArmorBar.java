package com.xuebi1145.xuplus_client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 使用CS2风格护甲图标替代原版icons.png护甲显示。
 * 盾牌图标在血量数字左侧（原版布局），低耐久时耐久数字发光变红。
 *
 * 图标资源（透明底）：
 * - Shield.png: 115x125
 * - Helmet.png: 150x100
 */
@Mixin(value = CSGameHud.class, remap = false)
public class MixinCSGameHudArmorBar {

    private static final ResourceLocation SHIELD_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/icon/shield.png");
    private static final ResourceLocation HELMET_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/icon/helmet.png");
    private static final int SHIELD_TEX_W = 115;
    private static final int SHIELD_TEX_H = 125;
    private static final int HELMET_TEX_W = 150;
    private static final int HELMET_TEX_H = 100;

    private static final int ICON_RENDER_H = 18;

    // 低耐久阈值（百分比）
    private static final float LOW_DURABILITY_THRESHOLD = 25f;

    @Inject(method = "renderArmorBar", at = @At("HEAD"), cancellable = true, remap = false)
    private void xuplus$renderArmorBarFromWeaponData(Minecraft mc, net.minecraftforge.client.gui.overlay.ForgeGui gui,
                                                     GuiGraphics guiGraphics, int healthTextX, int healthTextY,
                                                     CallbackInfo ci) {
        if (mc.player == null) return;

        ItemStack chestplate = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack helmetItem = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        boolean hasChestplate = !chestplate.isEmpty() && chestplate.getItem() instanceof ArmorItem;
        boolean hasHelmet = !helmetItem.isEmpty() && helmetItem.getItem() instanceof ArmorItem;

        if (!hasChestplate && !hasHelmet) {
            ci.cancel();
            return;
        }

        Font font = mc.font;

        // 计算胸甲耐久百分比
        int durabilityPercent = 100;
        if (hasChestplate) {
            int maxDmg = chestplate.getMaxDamage();
            int dmg = chestplate.getDamageValue();
            durabilityPercent = maxDmg > 0 ? Math.round((1f - (float) dmg / maxDmg) * 100) : 100;
        }

        // 盾牌缩放比
        float shieldScale = (float) ICON_RENDER_H / SHIELD_TEX_H;
        int shieldRenderW = Math.round(SHIELD_TEX_W * shieldScale);
        int shieldRenderH = ICON_RENDER_H;

        // 原版布局：盾牌在血量数字左侧
        int shieldX = healthTextX - shieldRenderW - 2;
        int shieldY = healthTextY;

        // 画盾牌（有胸甲时），若有头盔则稍微裁剪顶部左右边缘
        if (hasChestplate) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(shieldX, shieldY, 0);
            guiGraphics.pose().scale(shieldScale, shieldScale, 1);
            if (hasHelmet) {
                int helmetRenderH = (int) (ICON_RENDER_H / 4 * 1.5f);
                float hScale = (float) helmetRenderH / HELMET_TEX_H;
                int helmetRenderW = Math.round(HELMET_TEX_W * hScale);
                int clipW = (shieldRenderW - helmetRenderW) / 2;
                if (clipW > 0) {
                    guiGraphics.blit(SHIELD_ICON, 0, 0, 0, 0, SHIELD_TEX_W, SHIELD_TEX_H, SHIELD_TEX_W, SHIELD_TEX_H);
                    guiGraphics.pose().popPose();
                    RenderSystem.disableBlend();
                    guiGraphics.fill(shieldX, shieldY, shieldX + clipW, shieldY + 1, 0x00000000);
                    guiGraphics.fill(shieldX + shieldRenderW - clipW, shieldY, shieldX + shieldRenderW, shieldY + 1, 0x00000000);
                } else {
                    guiGraphics.blit(SHIELD_ICON, 0, 0, 0, 0, SHIELD_TEX_W, SHIELD_TEX_H, SHIELD_TEX_W, SHIELD_TEX_H);
                    guiGraphics.pose().popPose();
                    RenderSystem.disableBlend();
                }
            } else {
                guiGraphics.blit(SHIELD_ICON, 0, 0, 0, 0, SHIELD_TEX_W, SHIELD_TEX_H, SHIELD_TEX_W, SHIELD_TEX_H);
                guiGraphics.pose().popPose();
                RenderSystem.disableBlend();
            }
        }

        // 画头盔（有头盔时，居中于盾牌上方）
        if (hasHelmet) {
            int helmetRenderH = (int) (ICON_RENDER_H / 4 * 1.5f);
            float helmetScale = (float) helmetRenderH / HELMET_TEX_H;
            int helmetRenderW = Math.round(HELMET_TEX_W * helmetScale);
            int helmetX = shieldX + (shieldRenderW - helmetRenderW) / 2;
            int helmetY = shieldY - helmetRenderH / 3;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(helmetX, helmetY, 0);
            guiGraphics.pose().scale(helmetScale, helmetScale, 1);
            guiGraphics.blit(HELMET_ICON, 0, 0, 0, 0, HELMET_TEX_W, HELMET_TEX_H, HELMET_TEX_W, HELMET_TEX_H);
            guiGraphics.pose().popPose();
            RenderSystem.disableBlend();
        }

        // 耐久数字（画在盾牌图标内部居中，低耐久时发光变红）
        if (hasChestplate) {
            String text = String.valueOf(durabilityPercent);
            int width = font.width(text);
            float textScale = 1f;
            boolean lowDurability = durabilityPercent > 0 && durabilityPercent < LOW_DURABILITY_THRESHOLD;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(
                    shieldX + shieldRenderW / 2f - (width * textScale) / 2f,
                    shieldY + shieldRenderH / 2f - (font.lineHeight * textScale) / 2f,
                    0);
            guiGraphics.pose().scale(textScale, textScale, 1);

            if (lowDurability) {
                float maxRadius = 2.0f;
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        float dist = (float) Math.sqrt(dx * dx + dy * dy);
                        if (dist > maxRadius) continue;
                        float decay = (float) Math.exp(-2.5f * dist);
                        int alpha = (int) (0xFF * decay);
                        if (alpha <= 0) continue;
                        guiGraphics.drawString(font, text, dx, dy, (alpha << 24) | 0x00FF3333, true);
                    }
                }
                guiGraphics.drawString(font, text, 0, 0, 0xFFFF3333, false);
            } else {
                guiGraphics.drawString(font, text, 0, 0, 0xFFFFFFFF, false);
            }
            guiGraphics.pose().popPose();
        }

        ci.cancel();
    }
}

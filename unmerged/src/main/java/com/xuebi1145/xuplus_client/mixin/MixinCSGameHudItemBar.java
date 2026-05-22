package com.xuebi1145.xuplus_client.mixin;

import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameHud;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修改CSGameHud.renderSlot：
 * 选中物品时图标放大+提亮，切走时恢复原参数
 */
@Mixin(value = CSGameHud.class, remap = false)
public class MixinCSGameHudItemBar {

    @Unique
    private static final float xuplus$SELECTED_SCALE = 1.3f;

    @Unique
    private static final float xuplus$SELECTED_BRIGHTNESS = 1.4f;

    @Unique
    private static final long xuplus$ANIM_DURATION = 150;

    @Unique
    private static final long[] xuplus$slotAnimStart = new long[9];

    @Unique
    private static final boolean[] xuplus$itemActive = new boolean[9];

    @Unique
    private static final boolean[] xuplus$wasSelected = new boolean[9];

    @Inject(method = "renderSlot", at = @At("HEAD"), remap = false)
    private void xuplus$beforeRenderSlot(GuiGraphics guiGraphics, Font font, Inventory inv, int slotIndex,
                                          int x, int y, int width, int height,
                                          int bgColor, int textColor, CallbackInfo ci) {
        ItemStack stack = inv.getItem(slotIndex);
        boolean isSelected = inv.selected == slotIndex;

        // 检测状态切换
        if (isSelected && !xuplus$wasSelected[slotIndex]) {
            xuplus$slotAnimStart[slotIndex] = System.currentTimeMillis();
            xuplus$itemActive[slotIndex] = true;
        } else if (!isSelected && xuplus$wasSelected[slotIndex]) {
            xuplus$slotAnimStart[slotIndex] = System.currentTimeMillis();
        }
        xuplus$wasSelected[slotIndex] = isSelected;

        if (isSelected) {
            long elapsed = System.currentTimeMillis() - xuplus$slotAnimStart[slotIndex];
            float progress = Math.min(1f, (float) elapsed / xuplus$ANIM_DURATION);
            float eased = 1f - (1f - progress) * (1f - progress);
            float scale = 1f + (xuplus$SELECTED_SCALE - 1f) * eased;
            float brightness = 1f + (xuplus$SELECTED_BRIGHTNESS - 1f) * eased;

            float offsetX = (width - width * scale) / 2f;
            float offsetY = (height - height * scale) / 2f;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + offsetX, y + offsetY, 0);
            guiGraphics.pose().scale(scale, scale, 1);
            guiGraphics.pose().translate(-x, -y, 0);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(brightness, brightness, brightness, 1f);
        } else if (xuplus$itemActive[slotIndex]) {
            long elapsed = System.currentTimeMillis() - xuplus$slotAnimStart[slotIndex];
            float progress = Math.min(1f, (float) elapsed / xuplus$ANIM_DURATION);
            if (progress >= 1f) {
                xuplus$itemActive[slotIndex] = false;
                return;
            }
            float eased = 1f - (1f - progress) * (1f - progress);
            float scale = xuplus$SELECTED_SCALE + (1f - xuplus$SELECTED_SCALE) * eased;
            float brightness = xuplus$SELECTED_BRIGHTNESS + (1f - xuplus$SELECTED_BRIGHTNESS) * eased;

            float offsetX = (width - width * scale) / 2f;
            float offsetY = (height - height * scale) / 2f;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + offsetX, y + offsetY, 0);
            guiGraphics.pose().scale(scale, scale, 1);
            guiGraphics.pose().translate(-x, -y, 0);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(brightness, brightness, brightness, 1f);
        }
    }

    @Inject(method = "renderSlot", at = @At("TAIL"), remap = false)
    private void xuplus$afterRenderSlot(GuiGraphics guiGraphics, Font font, Inventory inv, int slotIndex,
                                         int x, int y, int width, int height,
                                         int bgColor, int textColor, CallbackInfo ci) {
        if (xuplus$itemActive[slotIndex]) {
            guiGraphics.pose().popPose();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }
}

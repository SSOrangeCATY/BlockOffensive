package com.xuebi1145.xuplus_client.hud.c4;

import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.xuebi1145.xuplus_client.hud.ClientBombTimerCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class C4TimerHUD {
    private int lastSeconds = -1;

    public void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            reset();
            return;
        }
        if (!shouldRenderHud()) {
            reset();
            return;
        }

        int totalFuseTicks = Math.max(1, ClientBombTimerCache.totalFuseTicks());
        int remainingTicks = Math.min(ClientBombTimerCache.fuseTicks(), totalFuseTicks);
        int remainingSeconds = (remainingTicks + 19) / 20;
        if (remainingSeconds <= 0) {
            reset();
            return;
        }

        playTickSound(remainingSeconds);
        int hudWidth = 230;
        int hudHeight = 30;
        int hudX = (screenWidth - hudWidth) / 2;
        int hudY = screenHeight - 112;
        renderCS2StyleHUD(guiGraphics, minecraft, hudX, hudY, hudWidth, hudHeight, remainingSeconds, remainingTicks, totalFuseTicks);
        lastSeconds = remainingSeconds;
    }

    private boolean shouldRenderHud() {
        return CSClientData.isStart
            && !CSClientData.isWaitingWinner
            && !CSClientData.isPause
            && !CSClientData.isWaiting
            && !CSClientData.isWarmTime
            && ClientBombTimerCache.active();
    }

    private void renderCS2StyleHUD(GuiGraphics guiGraphics, Minecraft minecraft, int hudX, int hudY, int hudWidth, int hudHeight, int remainingSeconds, int remainingTicks, int totalFuseTicks) {
        int bgColor;
        int lineColor;
        int textColor;
        if (remainingSeconds <= 5) {
            bgColor = 0xDD550000;
            lineColor = 0xFFFF3333;
            textColor = 0xFFFF3333;
        } else if (remainingSeconds <= 10) {
            bgColor = 0xDD331100;
            lineColor = 0xFFFF8800;
            textColor = 0xFFFF8800;
        } else {
            bgColor = 0xDD1A1A1A;
            lineColor = 0xFFFFD700;
            textColor = 0xFFFFFFFF;
        }

        guiGraphics.fill(hudX + 10, hudY, hudX + hudWidth - 10, hudY + hudHeight, bgColor);
        guiGraphics.fill(hudX + 7, hudY, hudX + 10, hudY + hudHeight, lineColor);
        guiGraphics.fill(hudX + hudWidth - 10, hudY, hudX + hudWidth - 7, hudY + hudHeight, lineColor);
        guiGraphics.fill(hudX + 18, hudY + hudHeight - 6, hudX + hudWidth - 18, hudY + hudHeight - 4, 0x553A3A3A);
        int progressWidth = Math.max(0, Math.min(hudWidth - 36, Math.round((hudWidth - 36) * (remainingTicks / (float) Math.max(1, totalFuseTicks)))));
        guiGraphics.fill(hudX + 18, hudY + hudHeight - 6, hudX + 18 + progressWidth, hudY + hudHeight - 4, lineColor);

        Component text = Component.translatable("xuplus.c4.timer", remainingSeconds);
        int textWidth = minecraft.font.width(text);
        int textX = hudX + (hudWidth - textWidth) / 2;
        int textY = hudY + 8;
        guiGraphics.drawString(minecraft.font, text, textX, textY, textColor, true);
    }

    private void playTickSound(int remainingSeconds) {
        if (lastSeconds != remainingSeconds && remainingSeconds <= 5) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                float pitch = 1.0F + (5 - remainingSeconds) * 0.15F;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch, 0.5F));
            }
        }
    }

    private void reset() {
        lastSeconds = -1;
    }
}

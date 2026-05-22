package com.xuebi1145.xuplus_client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.phasetranscrystal.blockoffensive.client.screen.hud.CSMvpHud;
import com.phasetranscrystal.blockoffensive.data.MvpReason;
import com.xuebi1145.xuplus_client.PlayerHeadTextureManager;
import com.xuebi1145.xuplus_client.PlayerVipManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Mixin到BlockOffensive的CSMvpHud
 * 布局: [玩家头像][音乐名称/作者(上下)][封面图]
 */
@Mixin(value = CSMvpHud.class, remap = false)
public class MixinCSMvpHud {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("XUPlus-PlayerHead");
    private static final ThreadLocal<String> CURRENT_PLAYER_NAME = new ThreadLocal<>();

    // 回合胜利阵营颜色：CT蓝、T黄
    @Unique
    private static int xuplus$teamColor = 0xFF3366FF;

    @Unique
    private static boolean xuplus$isCtTeam = true;

    // ===== @Shadow 字段（@Overwrite需要访问原类成员） =====
    @Shadow private static Font font;
    @Shadow private long colorTransitionStartTime;
    @Shadow private long mvpColorTransitionStartTime;
    @Shadow private UUID player;
    @Shadow private Component currentTeamName;
    @Shadow private Component currentPlayerName;
    @Shadow private Component mvpReason;
    @Shadow private Component extraInfo1;
    @Shadow private Component extraInfo2;

    // 常量
    @Shadow private static final int ROUND_BANNER_WIDTH = 400;
    @Shadow private static final int ROUND_BANNER_HEIGHT = 80;
    @Shadow private static final int MVP_PANEL_WIDTH = 580;
    @Shadow private static final int MVP_PANEL_HEIGHT = 90;
    @Shadow private static final int AVATAR_SIZE = 74;
    @Shadow private static final int COLOR_BAR_HEIGHT = 20;
    @Shadow private static final int COLOR_TRANSITION_DURATION = 150;
    @Shadow private static final int BASE_HEIGHT = 1080;

    // ===== @Shadow 方法 =====
    @Shadow private void renderScaledText(GuiGraphics g, PoseStack p, Component t, int x, int y, int c, float s) {}
    @Shadow private void renderAvatar(GuiGraphics g, int x, int y, float s) {}
    @Shadow private int lerpColor(int a, int b, float t) { return 0; }

    /**
     * 在triggerAnimation头部捕获获胜阵营信息（必须在append修改组件之前）
     */
    @Inject(method = "triggerAnimation", at = @At("HEAD"))
    private void xuplus$captureTeamInfo(MvpReason reason, CallbackInfo ci) {
        String winTeam = reason.getTeamName().getString().toUpperCase();
        xuplus$isCtTeam = winTeam.equals("CT");

        // 判断本地玩家是否在获胜方
        boolean isPlayerOnWinTeam;
        if (xuplus$isCtTeam) {
            isPlayerOnWinTeam = com.phasetranscrystal.fpsmatch.common.client.FPSMClient.getGlobalData().isCurrentTeam("ct");
        } else {
            isPlayerOnWinTeam = com.phasetranscrystal.fpsmatch.common.client.FPSMClient.getGlobalData().isCurrentTeam("t");
        }

        // 胜利绿色，失败红色
        xuplus$teamColor = isPlayerOnWinTeam ? 0xFF00FF00 : 0xFFFF0000;
    }

    /**
     * 在triggerAnimation尾部覆盖文本：胜方显示"回合胜利"，败方显示"回合失败"
     */
    @Inject(method = "triggerAnimation", at = @At("TAIL"))
    private void xuplus$overrideTeamName(MvpReason reason, CallbackInfo ci) {
        boolean isPlayerOnWinTeam;
        if (xuplus$isCtTeam) {
            isPlayerOnWinTeam = com.phasetranscrystal.fpsmatch.common.client.FPSMClient.getGlobalData().isCurrentTeam("ct");
        } else {
            isPlayerOnWinTeam = com.phasetranscrystal.fpsmatch.common.client.FPSMClient.getGlobalData().isCurrentTeam("t");
        }

        if (isPlayerOnWinTeam) {
            currentTeamName = Component.translatable("xuplus.round.victory").withStyle(net.minecraft.ChatFormatting.BOLD);
        } else {
            currentTeamName = Component.translatable("xuplus.round.defeat").withStyle(net.minecraft.ChatFormatting.BOLD);
        }
    }

    /**
     * @author XUPlus
     * @reason 回合胜利横幅：文本改为纯"回合胜利"，侧边条和文本颜色根据阵营变色
     */
    @Overwrite
    private void renderRoundVictoryBanner(GuiGraphics guiGraphics, PoseStack pose, float bannerProgress,
                                          float scaleFactor, int screenWidth, int screenHeight, long currentTime) {
        int scaledWidth = (int) (ROUND_BANNER_WIDTH * scaleFactor);
        int scaledHeight = (int) (ROUND_BANNER_HEIGHT * scaleFactor);
        int animatedWidth = (int) (scaledWidth * bannerProgress);
        int x = (screenWidth - animatedWidth) / 2;
        int y = (int) (190 * ((float) screenHeight / BASE_HEIGHT));

        // 背景颜色过渡
        int bgColor = 0xFFFFFFFF;
        if (colorTransitionStartTime != -1) {
            float colorProgress = Math.min((currentTime - colorTransitionStartTime) / (float) COLOR_TRANSITION_DURATION, 1f);
            bgColor = lerpColor(0xFFFFFFFF, 0xAA000000, colorProgress);
        }

        // 绘制背景面板
        guiGraphics.fill(x, y, x + animatedWidth, y + scaledHeight, bgColor);

        // 绘制队伍颜色侧边条（使用阵营颜色）
        int sideBarWidth = (int) (5 * scaleFactor);
        guiGraphics.fill(x, y, x + sideBarWidth, y + scaledHeight, xuplus$teamColor);
        guiGraphics.fill(x + animatedWidth - sideBarWidth, y, x + animatedWidth, y + scaledHeight, xuplus$teamColor);

        // 横幅完全展开后渲染文本
        if (bannerProgress >= 1f) {
            Component leftArrow = Component.literal("»").withStyle(ChatFormatting.BOLD);
            Component rightArrow = Component.literal("«").withStyle(ChatFormatting.BOLD);

            final float ARROW_SCALE = 3.0f;
            final float TEXT_SCALE = 3.0f;
            float combinedArrowScale = scaleFactor * ARROW_SCALE;
            float combinedTextScale = scaleFactor * TEXT_SCALE;

            int rightArrowWidth = (int) (font.width(rightArrow) * scaleFactor);
            int rightX = x + animatedWidth - sideBarWidth - (int) (10 * scaleFactor) - rightArrowWidth;
            int leftX = x + sideBarWidth + (int) (10 * scaleFactor);

            int textTotalHeight = (int) (font.lineHeight * combinedTextScale);
            int arrowTotalHeight = (int) (font.lineHeight * combinedArrowScale);
            int verticalOffset = (textTotalHeight - arrowTotalHeight) / 2;
            int textY = y + (scaledHeight - textTotalHeight) / 2 + verticalOffset;

            // 文本颜色（含透明度过渡）— 使用阵营颜色RGB
            int teamRgb = xuplus$teamColor & 0x00FFFFFF;
            int textColor = 0x00FFFFFF | teamRgb;
            if (colorTransitionStartTime != -1) {
                float alpha = Math.min((currentTime - colorTransitionStartTime) / 250f, 1f);
                textColor = (int) (alpha * 255) << 24 | teamRgb;
            }

            // 渲染左侧箭头
            pose.pushPose();
            pose.scale(combinedArrowScale, combinedArrowScale, 1f);
            guiGraphics.drawString(font, leftArrow,
                    (int) ((leftX / scaleFactor) / ARROW_SCALE),
                    (int) ((textY / scaleFactor) / ARROW_SCALE) - verticalOffset / 2,
                    textColor, false);
            pose.popPose();

            // 渲染右侧箭头
            pose.pushPose();
            pose.scale(combinedArrowScale, combinedArrowScale, 1f);
            guiGraphics.drawString(font, rightArrow,
                    (int) ((rightX / scaleFactor) / ARROW_SCALE),
                    (int) ((textY / scaleFactor) / ARROW_SCALE) - verticalOffset / 2,
                    textColor, false);
            pose.popPose();

            // 渲染中间文本（已改为纯"回合胜利"）
            int middleWidth = (int) (font.width(currentTeamName) * combinedTextScale);
            renderScaledText(guiGraphics, pose, currentTeamName,
                    x + (animatedWidth - middleWidth) / 2,
                    textY, textColor, combinedTextScale);
        }
    }

    /**
     * @author XUPlus
     * @reason MVP面板颜色条根据阵营变色
     */
    @Overwrite
    private void renderMVPInfoPanel(GuiGraphics guiGraphics, PoseStack pose, float progress,
                                    float scaleFactor, int screenWidth, int screenHeight, long currentTime) {
        int scaledPanelWidth = (int) (MVP_PANEL_WIDTH * scaleFactor);
        int scaledPanelHeight = (int) (MVP_PANEL_HEIGHT * scaleFactor);
        int yOffset = (int) ((ROUND_BANNER_HEIGHT + 16) * scaleFactor);
        int yPosition = (int) (190 * ((float) screenHeight / BASE_HEIGHT)) + yOffset;

        int animatedWidth = (int) (scaledPanelWidth * progress);
        int x = (screenWidth - animatedWidth) / 2;

        // 背景颜色过渡
        int bgColor = 0xFFFFFFFF;
        if (progress >= 1f) {
            if (mvpColorTransitionStartTime == -1) {
                mvpColorTransitionStartTime = currentTime;
            }
            float colorProgress = Math.min((currentTime - mvpColorTransitionStartTime) / (float) COLOR_TRANSITION_DURATION, 1f);
            bgColor = lerpColor(0xFFFFFFFF, 0xAA000000, colorProgress);
        }

        // 绘制面板背景
        guiGraphics.fill(x, yPosition, x + animatedWidth, yPosition + scaledPanelHeight, bgColor);

        // 面板未完全展开时不渲染内容
        if (progress < 1) return;

        // 渲染玩家头像
        int avatarX = x + (int) (110 * scaleFactor);
        int avatarY = yPosition + (scaledPanelHeight - (int) (AVATAR_SIZE * scaleFactor)) / 2;
        renderAvatar(guiGraphics, avatarX, avatarY, scaleFactor);

        // 信息区域起始位置
        int infoStartX = avatarX + (int) ((AVATAR_SIZE + 8) * scaleFactor);

        // 渲染MVP原因颜色条（使用阵营颜色）
        int reasonWidth = (int) (font.width(mvpReason) * scaleFactor);
        int padding = (int) (8 * scaleFactor);
        int colorBarWidth = reasonWidth + padding * 2;
        guiGraphics.fill(infoStartX, avatarY,
                infoStartX + colorBarWidth,
                avatarY + (int) (COLOR_BAR_HEIGHT * scaleFactor),
                0x773B8EDB);

        // 渲染MVP原因文本
        renderScaledText(guiGraphics, pose, mvpReason,
                infoStartX + padding,
                avatarY + (int) (6 * scaleFactor),
                0xFFFFFFFF, scaleFactor);

        // 渲染玩家名称
        float nameScale = scaleFactor * 2.0f;
        int nameY = avatarY + (int) (COLOR_BAR_HEIGHT * scaleFactor) + (int) (10 * scaleFactor) - 3;
        renderScaledText(guiGraphics, pose, currentPlayerName,
                infoStartX, nameY, 0xFFFFFFFF, nameScale);

        // 渲染额外信息1
        renderScaledText(guiGraphics, pose, extraInfo1,
                infoStartX,
                nameY + (int) (font.lineHeight * scaleFactor * 1.4f) + 8,
                0xFFFFFFFF,
                scaleFactor * 1.1f);

        // 渲染额外信息2
        int extraInfo2Y = nameY + (int) (font.lineHeight * nameScale) + (int) (4 * scaleFactor) + 12;
        renderScaledText(guiGraphics, pose, extraInfo2,
                infoStartX,
                extraInfo2Y,
                0xFFFFFFFF,
                scaleFactor * 1.1f);
    }

    @Inject(method = "renderAvatar", at = @At("HEAD"))
    private void capturePlayerName(GuiGraphics guiGraphics, int x, int y, float scaleFactor, CallbackInfo ci) {
        if (player != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player);
                if (playerInfo != null) {
                    CURRENT_PLAYER_NAME.set(playerInfo.getProfile().getName());
                }
            }
        }
    }
    
    @Redirect(
        method = "renderAvatar",
        at = @At(value = "INVOKE", 
                 target = "Lnet/minecraft/client/gui/components/PlayerFaceRenderer;m_280354_(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;III)V")
    )
    private void redirectPlayerFaceDraw(GuiGraphics guiGraphics, ResourceLocation skinTexture, int x, int y, int size) {
        String playerName = CURRENT_PLAYER_NAME.get();
        
        ResourceLocation customHead = getCustomHeadForPlayer(playerName, skinTexture);
        if (customHead != null && !customHead.equals(skinTexture)) {
            guiGraphics.blit(customHead, x, y, size, size, 0, 0, size, size, size, size);
        } else {
            PlayerFaceRenderer.draw(guiGraphics, skinTexture, x, y, size);
        }
        
        CURRENT_PLAYER_NAME.remove();
    }
    
    @Inject(method = "renderAvatar", at = @At("RETURN"))
    private void afterRenderAvatar(GuiGraphics guiGraphics, int x, int y, float scaleFactor, CallbackInfo ci) {
        if (player == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int avatarSize = (int) (74 * scaleFactor);
        int gap = (int) (6 * scaleFactor);
        
        PlayerVipManager.VipInfo vipInfo = PlayerVipManager.getVipInfo(player);
        if (vipInfo == null) return;
        
        com.mojang.blaze3d.vertex.PoseStack pose = guiGraphics.pose();
        
        // === VIP等级标签 - 头像右上角，根据文字自动扩展 ===
        if (vipInfo.vipType > 0) {
            String rawVipText = stripColorCodes(PlayerVipManager.getVipDisplayName(vipInfo.vipType));
            int textWidth = font.width(rawVipText);
            int padding = (int) (3 * scaleFactor);
            int badgeWidth = (int) (textWidth * scaleFactor) + padding * 2;
            int badgeHeight = (int) (font.lineHeight * scaleFactor) + padding;
            int badgeX = x + avatarSize - badgeWidth;
            int badgeY = y - badgeHeight / 2;
            int vipColor = PlayerVipManager.getVipColor(vipInfo.vipType);
            
            guiGraphics.fill(badgeX - 1, badgeY - 1, badgeX + badgeWidth + 1, badgeY + badgeHeight + 1, 0x99000000);
            guiGraphics.fill(badgeX - 1, badgeY - 1, badgeX + badgeWidth + 1, badgeY, vipColor);
            pose.pushPose();
            pose.translate(badgeX + padding, badgeY + padding / 2, 0);
            pose.scale(scaleFactor, scaleFactor, 1f);
            guiGraphics.drawString(font, PlayerVipManager.getVipDisplayName(vipInfo.vipType), 0, 0, vipColor, true);
            pose.popPose();
        }
        
        if (vipInfo.musicName == null || vipInfo.musicName.isEmpty()) return;
        
        // 面板布局计算（与CSMvpHud.renderMVPInfoPanel一致）
        // 头像位于 panelX + 110*scaleFactor，面板宽580*scaleFactor
        // 左边留空110，右边对称也留空110
        int panelX = x - (int) (110 * scaleFactor);
        int panelWidth = (int) (580 * scaleFactor);
        int panelRight = panelX + panelWidth;
        int leftPadding = (int) (110 * scaleFactor);
        
        // === [封面图] - 面板右侧，与左侧头像对称（同样留空110） ===
        int coverSize = avatarSize;
        int coverX = panelRight - leftPadding - coverSize;
        int coverY = y;
        
        if (vipInfo.coverTexture != null) {
            guiGraphics.fill(coverX - 1, coverY - 1, coverX + coverSize + 1, coverY + coverSize + 1, 0xFF6c5ce7);
            float[] currentColor = RenderSystem.getShaderColor();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            guiGraphics.blit(vipInfo.coverTexture, coverX, coverY, 0, 0, coverSize, coverSize, coverSize, coverSize);
            RenderSystem.setShaderColor(currentColor[0], currentColor[1], currentColor[2], currentColor[3]);
        } else {
            guiGraphics.fill(coverX, coverY, coverX + coverSize, coverY + coverSize, 0x441a1a2e);
            float noteScale = scaleFactor * 2.0f;
            int noteW = (int) (font.width("♪") * noteScale);
            int noteH = (int) (font.lineHeight * noteScale);
            pose.pushPose();
            pose.translate(coverX + (coverSize - noteW) / 2, coverY + (coverSize - noteH) / 2, 0);
            pose.scale(noteScale, noteScale, 1f);
            guiGraphics.drawString(font, "♪", 0, 0, 0xFF6c5ce7, true);
            pose.popPose();
        }
        
        // === [音乐名称/作者] - 玩家名称下方，左对齐 ===
        // 与CSMvpHud布局一致：infoStartX = avatarX + (AVATAR_SIZE + 8) * scaleFactor
        int infoStartX = x + avatarSize + gap;
        // 玩家名称位置: nameY = avatarY + 20*scaleFactor + 10*scaleFactor - 3
        int playerNameY = y + (int) (20 * scaleFactor) + (int) (10 * scaleFactor) - 3;
        // 玩家名称高度(2.0f scale) + extraInfo行后，放置音乐信息
        int musicNameY = playerNameY + (int) (font.lineHeight * scaleFactor * 2.0f) + (int) (4 * scaleFactor);
        
        // 音乐名称（比玩家名称稍小 1.8f vs 2.0f）
        String musicName = vipInfo.musicName;
        float musicNameScale = scaleFactor * 1.8f;
        pose.pushPose();
        pose.translate(infoStartX, musicNameY, 0);
        pose.scale(musicNameScale, musicNameScale, 1f);
        guiGraphics.drawString(font, musicName, 0, 0, 0xFFa29bfe, true);
        pose.popPose();
        
        // 音乐作者（下方）
        if (vipInfo.customMemo != null && !vipInfo.customMemo.isEmpty()) {
            String authorText = "by " + vipInfo.customMemo;
            float authorScale = scaleFactor * 0.9f;
            int authorY = musicNameY + (int) (font.lineHeight * musicNameScale) + (int) (2 * scaleFactor);
            pose.pushPose();
            pose.translate(infoStartX, authorY, 0);
            pose.scale(authorScale, authorScale, 1f);
            guiGraphics.drawString(font, authorText, 0, 0, 0xFF888888, false);
            pose.popPose();
        }
    }
    
    private static String stripColorCodes(String text) {
        return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
    
    private ResourceLocation getCustomHeadForPlayer(String playerName, ResourceLocation fallback) {
        if (playerName == null) return null;
        
        if (!PlayerHeadTextureManager.isLoaded(playerName)) {
            PlayerHeadTextureManager.preloadPlayerHead(playerName);
        }
        
        ResourceLocation customHead = PlayerHeadTextureManager.getPlayerHeadTexture(playerName);
        if (customHead != null && !customHead.equals(fallback)) {
            return customHead;
        }
        return null;
    }
}

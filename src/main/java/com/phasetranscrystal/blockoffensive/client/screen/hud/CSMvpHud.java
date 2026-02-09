package com.phasetranscrystal.blockoffensive.client.screen.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.phasetranscrystal.blockoffensive.data.MvpReason;
import com.phasetranscrystal.blockoffensive.sound.BOSoundRegister;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public class CSMvpHud {
    // 修正Optionull拼写错误，使用标准Optional
    private static final Comparator<PlayerInfo> PLAYER_COMPARATOR = Comparator.<PlayerInfo>comparingInt((p_253306_) -> {
        return p_253306_.getGameMode() == GameType.SPECTATOR ? 1 : 0;
    }).thenComparing((p_269613_) -> {
        return Optional.ofNullable(p_269613_.getTeam()).map(PlayerTeam::getName).orElse("");
    }).thenComparing((p_253305_) -> {
        return p_253305_.getProfile().getName();
    }, String::compareToIgnoreCase);

    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final Font font = minecraft.font;

    // 动画时长配置
    private static final int ROUND_BANNER_DURATION = 300;
    private static final int MVP_PANEL_DURATION = 500;
    private static final int COLOR_TRANSITION_DURATION = 150;
    private static final int CLOSING_ANIMATION_DURATION = 300;
    private static final float CLOSING_SPEED_FACTOR = 2.0f;

    // 基础分辨率（用于缩放计算）
    private static final int BASE_WIDTH = 1920;
    private static final int BASE_HEIGHT = 1080;

    // UI尺寸配置（基础分辨率下）
    private static final int ROUND_BANNER_WIDTH = 400;
    private static final int ROUND_BANNER_HEIGHT = 80;
    private static final int MVP_PANEL_WIDTH = 580;
    private static final int MVP_PANEL_HEIGHT = 90;
    private static final int AVATAR_SIZE = 74;
    private static final int COLOR_BAR_HEIGHT = 20;

    // 动画状态
    private long roundBannerStartTime = -1;
    private long mvpInfoStartTime = -1;
    private long colorTransitionStartTime = -1;
    private long mvpColorTransitionStartTime = -1;
    private boolean animationPlaying = false;
    private long closeAnimationStartTime = -1;
    private boolean isClosing = false;

    // 玩家信息
    private UUID player;
    private Component currentPlayerName = Component.empty();
    private Component currentTeamName = Component.empty();
    private Component extraInfo1 = Component.empty();
    private Component extraInfo2 = Component.empty();
    private Component mvpReason = Component.empty();

    public void triggerAnimation(MvpReason reason) {
        this.player = reason.uuid;
        // 所有文本添加粗体样式
        this.currentTeamName = ((MutableComponent) reason.getTeamName())
                .append(Component.translatable("cs.game.winner.mvpNameSub"))
                .withStyle(ChatFormatting.BOLD);
        this.currentPlayerName = reason.getPlayerName().withStyle(ChatFormatting.BOLD);
        this.mvpReason = reason.getMvpReason().withStyle(ChatFormatting.BOLD);
        this.extraInfo1 = reason.getExtraInfo1().withStyle(ChatFormatting.BOLD);
        this.extraInfo2 = reason.getExtraInfo2().withStyle(ChatFormatting.BOLD);

        // 重置动画状态
        this.roundBannerStartTime = System.currentTimeMillis();
        this.mvpInfoStartTime = -1;
        this.colorTransitionStartTime = -1;
        this.mvpColorTransitionStartTime = -1;
        this.animationPlaying = true;

        // MVP音效播放（优化代码结构）
        boolean isCtTeam = reason.getTeamName().getString().equals("CT");
        if (minecraft.level != null && minecraft.player != null) {
            minecraft.level.playLocalSound(
                    minecraft.player.getOnPos().above(2),
                    isCtTeam ? BOSoundRegister.VOICE_CT_WIN.get() : BOSoundRegister.VOICE_T_WIN.get(),
                    SoundSource.VOICE,
                    1.0f,
                    1.0f,
                    false
            );
        }
    }

    public long getMvpInfoStartTime() {
        return mvpInfoStartTime;
    }

    /**
     * 渲染MVP HUD（适配MC原生GUI缩放）
     */
    public void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (isClosing) {
            renderCloseAnimation(guiGraphics, screenWidth, screenHeight);
            return;
        }

        if (!animationPlaying) return;

        long currentTime = System.currentTimeMillis();
        PoseStack pose = guiGraphics.pose();
        float scaleFactor = ((float) screenWidth / BASE_WIDTH);

        // 回合胜利横幅动画
        if (roundBannerStartTime != -1) {
            float bannerProgress = Math.min((currentTime - roundBannerStartTime) / (float) ROUND_BANNER_DURATION, 1f);
            renderRoundVictoryBanner(guiGraphics, pose, bannerProgress, scaleFactor,
                    screenWidth, screenHeight, currentTime);

            // 横幅动画完成后触发MVP面板动画
            if (bannerProgress >= 1f && mvpInfoStartTime == -1) {
                mvpInfoStartTime = currentTime + 500;
                colorTransitionStartTime = currentTime;
            }
        }

        // MVP信息面板动画
        if (mvpInfoStartTime != -1 && currentTime > mvpInfoStartTime && !currentPlayerName.getString().isEmpty()) {
            float mvpProgress = Math.min((currentTime - mvpInfoStartTime) / (float) MVP_PANEL_DURATION, 1f);
            renderMVPInfoPanel(guiGraphics, pose, mvpProgress, scaleFactor, screenWidth, screenHeight, currentTime);
        }
    }

    /**
     * 渲染回合胜利横幅
     */
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

        // 绘制队伍颜色侧边条
        int teamColor = 0xFF3366FF;
        int sideBarWidth = (int) (5 * scaleFactor);
        guiGraphics.fill(x, y, x + sideBarWidth, y + scaledHeight, teamColor);
        guiGraphics.fill(x + animatedWidth - sideBarWidth, y, x + animatedWidth, y + scaledHeight, teamColor);

        // 横幅完全展开后渲染文本
        if (bannerProgress >= 1f) {
            // 箭头文本添加粗体
            Component leftArrow = Component.literal("»").withStyle(ChatFormatting.BOLD);
            Component rightArrow = Component.literal("«").withStyle(ChatFormatting.BOLD);

            final float ARROW_SCALE = 3.0f;
            final float TEXT_SCALE = 3.0f;
            float combinedArrowScale = scaleFactor * ARROW_SCALE;
            float combinedTextScale = scaleFactor * TEXT_SCALE;

            int rightArrowWidth = (int) (font.width(rightArrow) * scaleFactor);
            int rightX = x + animatedWidth - sideBarWidth - (int) (10 * scaleFactor) - rightArrowWidth;
            int leftX = x + sideBarWidth + (int) (10 * scaleFactor);

            // 垂直居中计算（补偿文本高度差）
            int textTotalHeight = (int) (font.lineHeight * combinedTextScale);
            int arrowTotalHeight = (int) (font.lineHeight * combinedArrowScale);
            int verticalOffset = (textTotalHeight - arrowTotalHeight) / 2;
            int textY = y + (scaledHeight - textTotalHeight) / 2 + verticalOffset;

            // 文本颜色（含透明度过渡）
            int textColor = 0x00FFFFFF;
            if (colorTransitionStartTime != -1) {
                float alpha = Math.min((currentTime - colorTransitionStartTime) / 250f, 1f);
                textColor = (int) (alpha * 255) << 24 | 0xFFFFFF;
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

            // 渲染中间队伍名称
            int middleWidth = (int) (font.width(currentTeamName) * combinedTextScale);
            renderScaledText(guiGraphics, pose, currentTeamName,
                    x + (animatedWidth - middleWidth) / 2,
                    textY, textColor, combinedTextScale);
        }
    }

    /**
     * 渲染MVP信息面板
     */
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

        // 渲染MVP原因颜色条
        int reasonWidth = (int) (font.width(mvpReason) * scaleFactor);
        int padding = (int) (8 * scaleFactor);
        int colorBarWidth = reasonWidth + padding * 2;
        guiGraphics.fill(infoStartX, avatarY,
                infoStartX + colorBarWidth,
                avatarY + (int) (COLOR_BAR_HEIGHT * scaleFactor),
                0x773366FF);

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

    /**
     * 渲染缩放后的文本（保持粗体样式）
     */
    private void renderScaledText(GuiGraphics guiGraphics, PoseStack pose, Component text,
                                  int x, int y, int color, float scale) {
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1f);
        guiGraphics.drawString(font, text, 0, 0, color, false);
        pose.popPose();
    }

    /**
     * 渲染玩家头像（适配缩放）
     */
    private void renderAvatar(GuiGraphics guiGraphics, int x, int y, float scaleFactor) {
        int scaledSize = (int) (AVATAR_SIZE * scaleFactor);
        ResourceLocation defaultAvatar = ResourceLocation.tryBuild("fpsmatch", "textures/ui/avatar.png");
        PlayerInfo info = getPlayerInfoByUUID(this.player);

        if (info != null) {
            PlayerFaceRenderer.draw(guiGraphics, info.getSkinLocation(), x, y, scaledSize);
        } else {
            if (defaultAvatar != null) {
                guiGraphics.blit(defaultAvatar, x, y, scaledSize, scaledSize, 0, 0, 64, 64, 64, 64);
            }
        }
    }

    /**
     * 颜色插值（用于过渡动画）
     */
    private int lerpColor(int startColor, int endColor, float progress) {
        int startA = (startColor >> 24) & 0xFF;
        int startR = (startColor >> 16) & 0xFF;
        int startG = (startColor >> 8) & 0xFF;
        int startB = startColor & 0xFF;

        int endA = (endColor >> 24) & 0xFF;
        int endR = (endColor >> 16) & 0xFF;
        int endG = (endColor >> 8) & 0xFF;
        int endB = endColor & 0xFF;

        return ((int) (startA + (endA - startA) * progress) << 24) |
                ((int) (startR + (endR - startR) * progress) << 16) |
                ((int) (startG + (endG - startG) * progress) << 8) |
                (int) (startB + (endB - startB) * progress);
    }

    /**
     * 渲染关闭动画（适配MC缩放）
     */
    private void renderCloseAnimation(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        long currentTime = System.currentTimeMillis();
        float progress = Math.min((currentTime - closeAnimationStartTime) / (float) CLOSING_ANIMATION_DURATION, 1f);

        float scaleFactor = ((float) screenWidth / BASE_WIDTH);

        // 颜色过渡（更快变白）
        int bgColor = lerpColor(0xAA000000, 0xFFFFFFFF, Math.min(progress * CLOSING_SPEED_FACTOR, 1f));

        // 缓动收缩比例（动画更自然）
        float closingRatio = (float) Math.pow(progress, 0.8);

        // 渲染横幅关闭动画
        renderClosingBanner(guiGraphics, screenWidth, screenHeight, closingRatio, bgColor, scaleFactor);

        // 渲染MVP面板关闭动画
        renderClosingPanel(guiGraphics, screenWidth, screenHeight, closingRatio, bgColor, scaleFactor);

        // 动画完成后重置状态
        if (progress >= 1f) {
            resetAnimation();
        }
    }

    /**
     * 渲染关闭中的胜利横幅
     */
    private void renderClosingBanner(GuiGraphics guiGraphics, int screenWidth, int screenHeight,
                                     float closingRatio, int color, float scaleFactor) {
        int originalWidth = (int) (ROUND_BANNER_WIDTH * scaleFactor);
        int currentWidth = (int) (originalWidth * (1 - closingRatio));
        int yPos = (int) (190 * ((float) screenHeight / BASE_HEIGHT));
        int centerX = screenWidth / 2;

        // 对称收缩
        int leftStart = centerX - currentWidth / 2;
        int rightEnd = centerX + currentWidth / 2;

        guiGraphics.fill(leftStart, yPos,
                rightEnd,
                yPos + (int) (ROUND_BANNER_HEIGHT * scaleFactor),
                color);
    }

    /**
     * 渲染关闭中的MVP面板
     */
    private void renderClosingPanel(GuiGraphics guiGraphics, int screenWidth, int screenHeight,
                                    float closingRatio, int color, float scaleFactor) {
        int originalWidth = (int) (MVP_PANEL_WIDTH * scaleFactor);
        int currentWidth = (int) (originalWidth * (1 - closingRatio));
        int bannerY = (int) (190 * ((float) screenHeight / BASE_HEIGHT));
        int panelY = bannerY + (int) ((ROUND_BANNER_HEIGHT + 16) * scaleFactor);

        // 对称收缩
        int centerX = screenWidth / 2;
        int leftStart = centerX - currentWidth / 2;
        int rightEnd = centerX + currentWidth / 2;

        guiGraphics.fill(leftStart, panelY,
                rightEnd,
                panelY + (int) (MVP_PANEL_HEIGHT * scaleFactor),
                color);
    }

    /**
     * 触发关闭动画
     */
    public void triggerCloseAnimation() {
        if (!animationPlaying) return;
        CSGameHud.getInstance().stopKillAnim();
        isClosing = true;
        closeAnimationStartTime = System.currentTimeMillis();
    }

    /**
     * 重置所有动画状态
     */
    public void resetAnimation() {
        closeAnimationStartTime = -1;
        isClosing = false;
        roundBannerStartTime = -1;
        mvpInfoStartTime = -1;
        colorTransitionStartTime = -1;
        mvpColorTransitionStartTime = -1;
        animationPlaying = false;
        currentPlayerName = Component.empty();
        currentTeamName = Component.empty();
        extraInfo1 = Component.empty();
        extraInfo2 = Component.empty();
        player = null;
    }

    /**
     * 根据UUID获取玩家信息
     */
    public PlayerInfo getPlayerInfoByUUID(UUID uuid) {
        return minecraft.player.connection.getListedOnlinePlayers().stream()
                .filter(playerInfo -> playerInfo.getProfile().getId().equals(uuid))
                .findFirst()
                .orElse(null);
    }
}
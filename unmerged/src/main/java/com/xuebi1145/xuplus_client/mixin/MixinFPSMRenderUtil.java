package com.xuebi1145.xuplus_client.mixin;

import com.mojang.authlib.GameProfile;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.util.RenderUtil;
import com.xuebi1145.xuplus_client.PlayerHeadTextureManager;
import com.xuebi1145.xuplus_client.bot.client.BotClientEntry;
import com.xuebi1145.xuplus_client.bot.client.BotClientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * 接入 FPSMatch 渲染工具：补充 Bot 的 PlayerInfo，并替换可用的玩家头像。
 */
@Mixin(value = RenderUtil.class, remap = false)
public class MixinFPSMRenderUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger("XUPlus-PlayerHead");

    @Inject(method = "getPlayerInfos", at = @At("RETURN"), cancellable = true, remap = false)
    private static void xuplus$appendBotInfos(CallbackInfoReturnable<List<PlayerInfo>> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }

        List<PlayerInfo> merged = new ArrayList<>(cir.getReturnValue());
        for (BotClientEntry entry : BotClientRegistry.entries()) {
            PlayerInfo info = findOrCreate(entry);
            if (info != null && merged.stream().noneMatch(existing -> existing.getProfile().getId().equals(entry.uuid()))) {
                merged.add(info);
            }
        }
        merged.sort(FPSMClient.PLAYER_COMPARATOR);
        cir.setReturnValue(merged.stream().limit(80L).toList());
    }

    @Inject(
        method = "renderTexture(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;IIIIZZ)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void onRenderTexture(GuiGraphics guiGraphics, ResourceLocation texture,
                                        int x, int y, int width, int height,
                                        boolean flipHorizontal, boolean flipVertical,
                                        CallbackInfo ci) {
        if (!texture.getPath().contains("skins/")) {
            return;
        }

        String playerName = getPlayerNameFromSkinTexture(texture);
        if (playerName == null) {
            return;
        }

        if (PlayerHeadTextureManager.isLoaded(playerName)) {
            ResourceLocation customHead = PlayerHeadTextureManager.getPlayerHeadTexture(playerName);
            if (customHead != null && !customHead.equals(texture)) {
                LOGGER.debug("替换玩家头像: {} -> {}", playerName, customHead);
                RenderUtil.renderTexture(guiGraphics, customHead, x, y, width, height, flipHorizontal, flipVertical);
                ci.cancel();
            }
            return;
        }

        PlayerHeadTextureManager.preloadPlayerHead(playerName);
    }

    private static String getPlayerNameFromSkinTexture(ResourceLocation skinTexture) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            return null;
        }
        for (PlayerInfo playerInfo : mc.getConnection().getOnlinePlayers()) {
            if (playerInfo.getSkinLocation().equals(skinTexture)) {
                return playerInfo.getProfile().getName();
            }
        }
        return null;
    }

    private static PlayerInfo findOrCreate(BotClientEntry entry) {
        Minecraft mc = Minecraft.getInstance();
        PlayerInfo existing = mc.getConnection().getPlayerInfo(entry.uuid());
        if (existing != null) {
            return existing;
        }
        try {
            return new PlayerInfo(new GameProfile(entry.uuid(), entry.name()), true);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

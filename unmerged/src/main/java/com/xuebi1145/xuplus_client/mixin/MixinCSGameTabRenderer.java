package com.xuebi1145.xuplus_client.mixin;

import com.mojang.authlib.GameProfile;
import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameTabRenderer;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.team.BaseTeam;
import com.phasetranscrystal.fpsmatch.util.RenderUtil;
import com.xuebi1145.xuplus_client.PlayerHeadTextureManager;
import com.xuebi1145.xuplus_client.bot.BotManager;
import com.xuebi1145.xuplus_client.bot.client.BotClientEntry;
import com.xuebi1145.xuplus_client.bot.client.BotClientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Mixin(value = CSGameTabRenderer.class, remap = false)
public abstract class MixinCSGameTabRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("XUPlus-PlayerHead");
    private static final ThreadLocal<String> CURRENT_PLAYER_NAME = new ThreadLocal<>();
    private static final ResourceLocation GUI_ICONS_LOCATION = ResourceLocation.tryBuild("minecraft", "textures/gui/icons.png");
    private static final ResourceLocation BOT_ICON = ResourceLocation.tryBuild("xuplus_client", "textures/cs2/bot.png");
    private static final Map<String, String> MAP_TRANSLATION_KEYS = new HashMap<>();

    static {
        MAP_TRANSLATION_KEYS.put("dust2", "xuplus.map.hot_sand_ii");
        MAP_TRANSLATION_KEYS.put("dust_2", "xuplus.map.hot_sand_ii");
        MAP_TRANSLATION_KEYS.put("de_dust2", "xuplus.map.hot_sand_ii");
        MAP_TRANSLATION_KEYS.put("desert", "xuplus.map.hot_sand_ii");
        MAP_TRANSLATION_KEYS.put("hot_sand_ii", "xuplus.map.hot_sand_ii");
        MAP_TRANSLATION_KEYS.put("mirage", "xuplus.map.mirage");
        MAP_TRANSLATION_KEYS.put("inferno", "xuplus.map.inferno");
        MAP_TRANSLATION_KEYS.put("nuke", "xuplus.map.nuke");
        MAP_TRANSLATION_KEYS.put("ancient", "xuplus.map.ancient");
        MAP_TRANSLATION_KEYS.put("anubis", "xuplus.map.anubis");
        MAP_TRANSLATION_KEYS.put("overpass", "xuplus.map.overpass");
        MAP_TRANSLATION_KEYS.put("vertigo", "xuplus.map.vertigo");
        MAP_TRANSLATION_KEYS.put("office", "xuplus.map.office");
        MAP_TRANSLATION_KEYS.put("italy", "xuplus.map.italy");
        MAP_TRANSLATION_KEYS.put("train", "xuplus.map.train");
        MAP_TRANSLATION_KEYS.put("cache", "xuplus.map.cache");
    }

    @Shadow protected Minecraft minecraft;
    @Shadow protected abstract Component getNameForDisplay(PlayerInfo info);

    @Inject(method = "renderPlayerRow", at = @At("HEAD"))
    private void capturePlayerName(GuiGraphics guiGraphics, PlayerInfo player, int x, int y, int width, int height, int textColor, CallbackInfo ci) {
        CURRENT_PLAYER_NAME.set(player.getProfile().getName());
    }

    @Redirect(
        method = "renderPlayerRow",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/PlayerFaceRenderer;m_280354_(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;III)V")
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

    /**
     * @author OpenAI
     * @reason Replace the fragile custom tab layout with a stable version that tolerates missing bot PlayerData and still shows bot stats.
     */
    @Overwrite
    public void render(GuiGraphics guiGraphics, int windowWidth, List<PlayerInfo> playerInfoList, Scoreboard scoreboard, Objective objective) {
        if (minecraft == null || minecraft.getWindow() == null) {
            return;
        }

        List<PlayerInfo> mergedPlayers = mergeBotPlayers(playerInfoList);
        Map<String, List<PlayerInfo>> teamPlayers = groupPlayers(mergedPlayers);
        Comparator<PlayerInfo> damageComparator = Comparator.comparingDouble((PlayerInfo info) -> getDamageSafe(info)).reversed();
        teamPlayers.get("ct").sort(damageComparator);
        teamPlayers.get("t").sort(damageComparator);

        int padding = 5;
        int pingWidth = 40;
        int avatarSize = 12;
        int nameWidth = 110;
        int moneyWidth = 40;
        int killWidth = 35;
        int deathWidth = 35;
        int assistWidth = 35;
        int headshotWidth = 40;
        int damageWidth = 48;

        int playerAreaWidth = 400;
        int playerRowHeight = 12;
        int playerGap = 2;
        int headerHeight = 12;
        int teamGap = 10;

        int ctPlayerCount = teamPlayers.get("ct").size();
        int tPlayerCount = teamPlayers.get("t").size();
        int ctContentHeight = ctPlayerCount > 0 ? (playerRowHeight + playerGap) * ctPlayerCount - playerGap : 0;
        int tContentHeight = tPlayerCount > 0 ? (playerRowHeight + playerGap) * tPlayerCount - playerGap : 0;
        int totalContentHeight = headerHeight + ctContentHeight + teamGap + tContentHeight;

        int bgPadding = 10;
        int bgWidth = playerAreaWidth + bgPadding * 2;
        int bgHeight = totalContentHeight + bgPadding * 2 + 18;
        int bgX = (windowWidth - bgWidth) / 2;
        int bgY = (minecraft.getWindow().getGuiScaledHeight() - bgHeight) / 2;

        guiGraphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0xA010141A);

        Component title = Component.translatable("xuplus.tab.title", mapDisplayName(FPSMClient.getGlobalData().getCurrentMap()));
        guiGraphics.drawString(minecraft.font, title, bgX + bgPadding, bgY + 2, 0xFFFFFFFF, false);
        String roundTime = currentRoundTimeText();
        guiGraphics.drawString(minecraft.font, roundTime, bgX + bgWidth - bgPadding - minecraft.font.width(roundTime), bgY + 2, 0xFFBAC1CA, false);

        int headerY = bgY + bgPadding + 12;
        int ctStartY = headerY + headerHeight + 2;
        int dividerY = ctStartY + ctContentHeight + teamGap / 2;
        int tStartY = ctStartY + ctContentHeight + teamGap;

        if (ctPlayerCount > 0 && tPlayerCount > 0) {
            guiGraphics.fill(bgX, dividerY, bgX + bgWidth, dividerY + 1, 0x40FFFFFF);
        }

        int currentHeaderX = bgX + bgPadding;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
        guiGraphics.blit(GUI_ICONS_LOCATION, currentHeaderX + (pingWidth - 10) / 2, headerY + 2, 0, 176, 10, 8);
        guiGraphics.pose().popPose();
        currentHeaderX += pingWidth;
        currentHeaderX += avatarSize + nameWidth + padding;

        Component moneyText = Component.translatable("xuplus.tab.money");
        guiGraphics.drawString(minecraft.font, moneyText, currentHeaderX + (moneyWidth - minecraft.font.width(moneyText)) / 2, headerY, 0xFFFFFFFF, false);
        currentHeaderX += moneyWidth;

        guiGraphics.drawString(minecraft.font, Component.literal("K"), currentHeaderX + (killWidth - minecraft.font.width("K")) / 2, headerY, 0xFFFFFFFF, false);
        currentHeaderX += killWidth;
        guiGraphics.drawString(minecraft.font, Component.literal("D"), currentHeaderX + (deathWidth - minecraft.font.width("D")) / 2, headerY, 0xFFFFFFFF, false);
        currentHeaderX += deathWidth;
        guiGraphics.drawString(minecraft.font, Component.literal("A"), currentHeaderX + (assistWidth - minecraft.font.width("A")) / 2, headerY, 0xFFFFFFFF, false);
        currentHeaderX += assistWidth;

        Component hsText = Component.translatable("xuplus.tab.hs");
        guiGraphics.drawString(minecraft.font, hsText, currentHeaderX + (headshotWidth - minecraft.font.width(hsText)) / 2, headerY, 0xFFFFFFFF, false);
        currentHeaderX += headshotWidth;

        Component damageText = Component.translatable("xuplus.tab.dmg");
        guiGraphics.drawString(minecraft.font, damageText, currentHeaderX + (damageWidth - minecraft.font.width(damageText)) / 2, headerY, 0xFFFFFFFF, false);

        if (ctPlayerCount == 0 && tPlayerCount == 0) {
            Component placeholder = Component.literal("Waiting for player data...");
            guiGraphics.drawString(minecraft.font, placeholder, bgX + (bgWidth - minecraft.font.width(placeholder)) / 2, headerY + 18, 0xFFBAC1CA, false);
            return;
        }

        int currentY = ctStartY;
        for (PlayerInfo ctPlayer : teamPlayers.get("ct")) {
            renderPlayerRowStable(guiGraphics, ctPlayer, bgX + bgPadding, currentY, playerAreaWidth, playerRowHeight, 0xFF8DDCFF);
            currentY += playerRowHeight + playerGap;
        }

        currentY = tStartY;
        for (PlayerInfo tPlayer : teamPlayers.get("t")) {
            renderPlayerRowStable(guiGraphics, tPlayer, bgX + bgPadding, currentY, playerAreaWidth, playerRowHeight, 0xFFF0D27E);
            currentY += playerRowHeight + playerGap;
        }
    }

    private void renderPlayerRowStable(GuiGraphics guiGraphics, PlayerInfo player, int x, int y, int width, int height, int textColor) {
        UUID uuid = player.getProfile().getId();
        Optional<PlayerData> data = FPSMClient.getGlobalData().getPlayerData(uuid);
        Optional<BotClientEntry> botEntry = BotClientRegistry.get(uuid);

        String playerTeam = resolveTeamName(player, botEntry);
        String localTeam = FPSMClient.getGlobalData().getCurrentTeam();
        boolean isSameTeam = Objects.equals(playerTeam, localTeam);
        boolean isLocalPlayer = minecraft.player != null && player.getProfile().getId().equals(minecraft.player.getUUID());

        int bgColor = isLocalPlayer ? (textColor & 0x00FFFFFF) | 0x40000000 : 0x40000000;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        int padding = 5;
        int pingWidth = 40;
        int avatarSize = 12;
        int nameWidth = 110;
        int moneyWidth = 40;
        int killWidth = 35;
        int deathWidth = 35;
        int assistWidth = 35;
        int headshotWidth = 40;
        int damageWidth = 48;

        int textY = y + (height - 8) / 2;
        int currentX = x;

        String pingText = String.valueOf(Math.max(0, player.getLatency()));
        guiGraphics.drawString(minecraft.font, pingText, currentX + (pingWidth - minecraft.font.width(pingText)) / 2, textY, RenderUtil.color(25, 180, 60), false);
        currentX += pingWidth;

        if ((BotManager.isBot(uuid) || BotClientRegistry.isBot(uuid)) && BOT_ICON != null) {
            guiGraphics.blit(BOT_ICON, currentX, y, 0, 0, avatarSize, avatarSize, avatarSize, avatarSize);
        } else {
            PlayerFaceRenderer.draw(guiGraphics, player.getSkinLocation(), currentX, y, avatarSize);
        }
        currentX += avatarSize + padding;

        guiGraphics.drawString(minecraft.font, getNameForDisplay(player), currentX, textY, textColor, false);

        int moneyX = x + pingWidth + avatarSize + padding + nameWidth;
        guiGraphics.fill(moneyX, y, moneyX + moneyWidth, y + height, 0x20FFFFFF);
        if (isSameTeam) {
            String money = "$" + getMoneySafe(uuid, botEntry);
            guiGraphics.drawString(minecraft.font, money, moneyX + (moneyWidth - minecraft.font.width(money)) / 2, textY, textColor, false);
        }

        int killsX = moneyX + moneyWidth;
        String kills = String.valueOf(getKillsSafe(uuid, data, botEntry));
        guiGraphics.drawString(minecraft.font, kills, killsX + (killWidth - minecraft.font.width(kills)) / 2, textY, textColor, false);

        int deathsX = killsX + killWidth;
        guiGraphics.fill(deathsX, y, deathsX + deathWidth, y + height, 0x20FFFFFF);
        String deaths = String.valueOf(getDeathsSafe(uuid, data, botEntry));
        guiGraphics.drawString(minecraft.font, deaths, deathsX + (deathWidth - minecraft.font.width(deaths)) / 2, textY, textColor, false);

        int assistsX = deathsX + deathWidth;
        String assists = String.valueOf(getAssistsSafe(uuid, data, botEntry));
        guiGraphics.drawString(minecraft.font, assists, assistsX + (assistWidth - minecraft.font.width(assists)) / 2, textY, textColor, false);

        int headshotX = assistsX + assistWidth;
        guiGraphics.fill(headshotX, y, headshotX + headshotWidth, y + height, 0x20FFFFFF);
        String headshotPercentage = String.format(Locale.ROOT, "%.0f%%", getHeadshotRateSafe(data) * 100.0F);
        guiGraphics.drawString(minecraft.font, headshotPercentage, headshotX + (headshotWidth - minecraft.font.width(headshotPercentage)) / 2, textY, textColor, false);

        int damageX = x + width - damageWidth;
        guiGraphics.fill(damageX, y, damageX + damageWidth, y + height, 0x40FFFFFF);
        String damage = String.valueOf(Math.round(getDamageSafe(uuid, data, botEntry)));
        guiGraphics.drawString(minecraft.font, damage, damageX + (damageWidth - minecraft.font.width(damage)) / 2, textY, textColor, false);

        if (!isLivingSafe(uuid, data, botEntry)) {
            guiGraphics.fill(x, y, x + width, y + height, 0x40000000);
        }
    }

    private List<PlayerInfo> mergeBotPlayers(List<PlayerInfo> playerInfoList) {
        List<PlayerInfo> merged = new ArrayList<>(playerInfoList == null ? List.of() : playerInfoList);
        if (minecraft == null || minecraft.getConnection() == null) {
            return merged;
        }

        for (BotClientEntry entry : BotClientRegistry.entries()) {
            boolean exists = merged.stream().anyMatch(info -> info.getProfile().getId().equals(entry.uuid()));
            if (exists) {
                continue;
            }
            PlayerInfo info = findOrCreate(entry);
            if (info != null) {
                merged.add(info);
            }
        }
        merged.sort(FPSMClient.PLAYER_COMPARATOR);
        return merged;
    }

    private Map<String, List<PlayerInfo>> groupPlayers(List<PlayerInfo> players) {
        Map<String, List<PlayerInfo>> grouped = new HashMap<>();
        grouped.put("ct", new ArrayList<>());
        grouped.put("t", new ArrayList<>());

        for (PlayerInfo info : players) {
            UUID uuid = info.getProfile().getId();
            String team = resolveTeamName(info, BotClientRegistry.get(uuid));
            if ("ct".equals(team)) {
                grouped.get("ct").add(info);
            } else if ("t".equals(team)) {
                grouped.get("t").add(info);
            }
        }
        return grouped;
    }

    private String resolveTeamName(PlayerInfo info, Optional<BotClientEntry> botEntry) {
        String team = FPSMClient.getGlobalData().getTeamByUUID(info.getProfile().getId()).map(BaseTeam::getName).orElse("");
        if (!team.isBlank()) {
            return normalizeTeam(team);
        }
        PlayerTeam scoreboardTeam = info.getTeam();
        if (scoreboardTeam != null && scoreboardTeam.getName() != null && !scoreboardTeam.getName().isBlank()) {
            return normalizeTeam(scoreboardTeam.getName());
        }
        return botEntry.map(BotClientEntry::team).map(this::normalizeTeam).orElse("");
    }

    private String resolveTeamName(UUID uuid, Optional<BotClientEntry> botEntry) {
        String team = FPSMClient.getGlobalData().getTeamByUUID(uuid).map(BaseTeam::getName).orElse("");
        if (!team.isBlank()) {
            return normalizeTeam(team);
        }
        return botEntry.map(BotClientEntry::team).map(this::normalizeTeam).orElse("");
    }

    private String normalizeTeam(String team) {
        if (team == null) {
            return "";
        }
        String normalized = team.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("ct") || normalized.equals("counterterrorist") || normalized.equals("counter_terrorist") || normalized.equals("counter-terrorist")) {
            return "ct";
        }
        if (normalized.equals("t") || normalized.equals("terrorist") || normalized.equals("terrorists")) {
            return "t";
        }
        return normalized;
    }

    private int getMoneySafe(UUID uuid, Optional<BotClientEntry> botEntry) {
        int money = FPSMClient.getGlobalData().getPlayerMoney(uuid);
        if (money > 0 || botEntry.isEmpty()) {
            return money;
        }
        return botEntry.get().money();
    }

    private int getKillsSafe(UUID uuid, Optional<PlayerData> data, Optional<BotClientEntry> botEntry) {
        return data.map(PlayerData::getKills).orElseGet(() -> botEntry.map(BotClientEntry::kills).orElse(FPSMClient.getGlobalData().getKills(uuid)));
    }

    private int getDeathsSafe(UUID uuid, Optional<PlayerData> data, Optional<BotClientEntry> botEntry) {
        return data.map(PlayerData::getDeaths).orElseGet(() -> botEntry.map(BotClientEntry::deaths).orElse(FPSMClient.getGlobalData().getDeaths(uuid)));
    }

    private int getAssistsSafe(UUID uuid, Optional<PlayerData> data, Optional<BotClientEntry> botEntry) {
        return data.map(PlayerData::getAssists).orElseGet(() -> botEntry.map(BotClientEntry::assists).orElse(FPSMClient.getGlobalData().getAssists(uuid)));
    }

    private double getDamageSafe(PlayerInfo info) {
        UUID uuid = info.getProfile().getId();
        return getDamageSafe(uuid, FPSMClient.getGlobalData().getPlayerData(uuid), BotClientRegistry.get(uuid));
    }

    private float getDamageSafe(UUID uuid, Optional<PlayerData> data, Optional<BotClientEntry> botEntry) {
        return data.map(PlayerData::getDamage).orElseGet(() -> botEntry.map(BotClientEntry::damage).orElse(FPSMClient.getGlobalData().getDamage(uuid)));
    }

    private boolean isLivingSafe(UUID uuid, Optional<PlayerData> data, Optional<BotClientEntry> botEntry) {
        return data.map(PlayerData::isLiving).orElseGet(() -> botEntry.map(BotClientEntry::living).orElse(FPSMClient.getGlobalData().isLiving(uuid)));
    }

    private float getHeadshotRateSafe(Optional<PlayerData> data) {
        return data.map(PlayerData::getHeadshotRate).orElse(0.0F);
    }

    private PlayerInfo findOrCreate(BotClientEntry entry) {
        if (minecraft == null || minecraft.getConnection() == null) {
            return null;
        }
        PlayerInfo existing = minecraft.getConnection().getPlayerInfo(entry.uuid());
        if (existing != null) {
            return existing;
        }
        try {
            return new PlayerInfo(new GameProfile(entry.uuid(), entry.name()), true);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String currentRoundTimeText() {
        if (!CSClientData.isStart || CSClientData.isWaitingWinner || CSClientData.time < 0) {
            return "00:00";
        }
        int totalSeconds = Math.max(0, CSClientData.time / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private Component mapDisplayName(String rawMap) {
        if (rawMap == null || rawMap.isBlank()) {
            return Component.translatable("xuplus.map.unknown");
        }
        String translationKey = MAP_TRANSLATION_KEYS.get(rawMap.toLowerCase(Locale.ROOT));
        if (translationKey != null && I18n.exists(translationKey)) {
            return Component.translatable(translationKey);
        }
        return Component.literal(rawMap);
    }

    private ResourceLocation getCustomHeadForPlayer(String playerName, ResourceLocation fallback) {
        if (CURRENT_PLAYER_NAME.get() != null && minecraft != null) {
            for (PlayerInfo info : FPSMClient.getPlayerInfos()) {
                UUID uuid = info.getProfile().getId();
                if (Objects.equals(info.getProfile().getName(), playerName) && (BotManager.isBot(uuid) || BotClientRegistry.isBot(uuid)) && BOT_ICON != null) {
                    return BOT_ICON;
                }
            }
        }
        if (playerName == null) {
            return null;
        }
        if (!PlayerHeadTextureManager.isLoaded(playerName)) {
            PlayerHeadTextureManager.preloadPlayerHead(playerName);
        }
        ResourceLocation customHead = PlayerHeadTextureManager.getPlayerHeadTexture(playerName);
        if (customHead != null && !customHead.equals(fallback)) {
            LOGGER.debug("[Tab] Found custom head for {}", playerName);
            return customHead;
        }
        return null;
    }
}

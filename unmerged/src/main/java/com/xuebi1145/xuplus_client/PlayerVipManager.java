package com.xuebi1145.xuplus_client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家 VIP 信息管理器，缓存网站身份等级和音乐盒信息。
 */
public class PlayerVipManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("XUPlus-Vip");
    private static final String API_URL = "http://t6.sjcmc.cn:35159/music/api/music_api.php?uuid=";
    private static final Gson GSON = new Gson();

    private static final ConcurrentHashMap<UUID, VipInfo> VIP_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ResourceLocation> COVER_CACHE = new ConcurrentHashMap<>();

    public static class VipInfo {
        public final int vipType;
        public final String customMemo;
        public final String musicName;
        public final String coverUrl;
        public final ResourceLocation coverTexture;

        public VipInfo(int vipType, String customMemo, String musicName, String coverUrl, ResourceLocation coverTexture) {
            this.vipType = vipType;
            this.customMemo = customMemo;
            this.musicName = musicName;
            this.coverUrl = coverUrl;
            this.coverTexture = coverTexture;
        }
    }

    public static String getVipDisplayName(int vipType) {
        return switch (vipType) {
            case 1 -> "§7普通";
            case 2 -> "§6§lPLUS";
            case 3 -> "§2协管";
            case 4 -> "§4§l管理";
            default -> "§7未知";
        };
    }

    public static int getVipColor(int vipType) {
        return switch (vipType) {
            case 0, 1 -> 0xFFAAAAAA;
            case 2 -> 0xFFFFAA00;
            case 3 -> 0xFF00AA00;
            case 4 -> 0xFFAA0000;
            default -> 0xFFAAAAAA;
        };
    }

    public static void updateFullInfo(UUID uuid, int vipType, String customMemo, String musicName, String coverUrl) {
        ResourceLocation coverTexture = loadCoverTexture(coverUrl);
        VIP_CACHE.put(uuid, new VipInfo(vipType, customMemo, musicName, coverUrl, coverTexture));
        LOGGER.info("更新玩家完整信息: UUID={}, vipType={}, music={}, cover={}", uuid, vipType, musicName, coverUrl);
    }

    public static void updateVipInfo(UUID uuid, int vipType, String customMemo) {
        VipInfo existing = VIP_CACHE.get(uuid);
        String musicName = existing != null ? existing.musicName : "";
        String coverUrl = existing != null ? existing.coverUrl : "";
        ResourceLocation coverTexture = existing != null ? existing.coverTexture : null;
        VIP_CACHE.put(uuid, new VipInfo(vipType, customMemo, musicName, coverUrl, coverTexture));
    }

    public static VipInfo getVipInfo(UUID uuid) {
        return VIP_CACHE.get(uuid);
    }

    public static int getVipType(UUID uuid) {
        VipInfo info = VIP_CACHE.get(uuid);
        return info != null ? info.vipType : -1;
    }

    private static ResourceLocation loadCoverTexture(String coverUrl) {
        if (coverUrl == null || coverUrl.isEmpty()) {
            return null;
        }

        ResourceLocation cached = COVER_CACHE.get(coverUrl);
        if (cached != null) {
            return cached;
        }

        new Thread(() -> downloadCoverTexture(coverUrl), "XUPlus-Cover-Loader").start();
        return null;
    }

    private static void downloadCoverTexture(String coverUrl) {
        try {
            URL url = new URL(coverUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            if (conn.getResponseCode() != 200) {
                return;
            }

            byte[] imageData;
            try (InputStream is = new BufferedInputStream(conn.getInputStream())) {
                imageData = is.readAllBytes();
            }

            Minecraft.getInstance().execute(() -> registerCoverTexture(coverUrl, imageData));
        } catch (Exception e) {
            LOGGER.debug("下载封面失败: {}", coverUrl, e);
        }
    }

    private static void registerCoverTexture(String coverUrl, byte[] imageData) {
        try {
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(imageData));
            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
            String textureId = "xuplus_client_cover_" + coverUrl.hashCode();
            ResourceLocation texture = Minecraft.getInstance().getTextureManager().register(textureId, dynamicTexture);
            COVER_CACHE.put(coverUrl, texture);

            VIP_CACHE.forEach((uuid, info) -> {
                if (coverUrl.equals(info.coverUrl)) {
                    VIP_CACHE.put(uuid, new VipInfo(info.vipType, info.customMemo, info.musicName, info.coverUrl, texture));
                }
            });
            LOGGER.info("加载封面纹理: {}", coverUrl);
        } catch (Exception e) {
            LOGGER.error("创建封面纹理失败: {}", coverUrl, e);
        }
    }

    public static void preloadVipInfo(UUID uuid) {
        if (VIP_CACHE.containsKey(uuid)) {
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(API_URL + uuid);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                if (conn.getResponseCode() != 200) {
                    return;
                }

                String response;
                try (InputStream is = conn.getInputStream();
                     java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8")) {
                    scanner.useDelimiter("\\A");
                    response = scanner.hasNext() ? scanner.next() : "";
                }

                JsonObject json = GSON.fromJson(response, JsonObject.class);
                if (json.has("ok") && json.get("ok").getAsBoolean()) {
                    JsonObject data = json.getAsJsonObject("data");
                    int vipType = data.has("vip_type") ? data.get("vip_type").getAsInt() : 0;
                    String customMemo = data.has("custom_memo") ? data.get("custom_memo").getAsString() : "";
                    String musicName = data.has("music_name") ? data.get("music_name").getAsString() : "";
                    String coverUrl = data.has("cover_url") ? data.get("cover_url").getAsString() : "";
                    int autofish = data.has("autofish") ? data.get("autofish").getAsInt() : 0;
                    updateFullInfo(uuid, vipType, customMemo, musicName, coverUrl);
                    LOGGER.info("预加载 VIP 信息: UUID={}, vipType={}, autofish={}", uuid, vipType, autofish);
                }
            } catch (Exception e) {
                LOGGER.debug("预加载 VIP 信息失败: {}", uuid, e);
            }
        }, "XUPlus-Vip-Preload").start();
    }

    public static void clearCache() {
        VIP_CACHE.clear();
        COVER_CACHE.clear();
    }
}

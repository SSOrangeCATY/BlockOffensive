package com.xuebi1145.xuplus_client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家头像纹理管理器，从 API 拉取头像并缓存。
 */
public class PlayerHeadTextureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerHeadTextureManager.class);
    private static final String API_URL = "http://t6.sjcmc.cn:35159/music/api/player_head.php?player=";

    private static final Map<String, Object> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Object>> LOADING = new ConcurrentHashMap<>();
    private static boolean isPreloading = false;

    /**
     * 预加载在线玩家头像。
     */
    public static void preloadAllPlayerHeads() {
        if (isPreloading) {
            return;
        }

        isPreloading = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            LOGGER.info("开始预加载玩家头像...");
            mc.getConnection().getOnlinePlayers().forEach(playerInfo -> {
                String playerName = playerInfo.getProfile().getName();
                if (!TEXTURE_CACHE.containsKey(playerName) && !LOADING.containsKey(playerName)) {
                    preloadPlayerHead(playerName);
                }
            });
            LOGGER.info("玩家头像预加载完成");
        }
        isPreloading = false;
    }

    /**
     * 预加载单个玩家头像。
     */
    public static void preloadPlayerHead(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return;
        }
        if (TEXTURE_CACHE.containsKey(playerName) || LOADING.containsKey(playerName)) {
            return;
        }

        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> loadPlayerHeadFromAPI(playerName));
        LOADING.put(playerName, future);
        future.thenAccept(texture -> {
            if (texture != null) {
                TEXTURE_CACHE.put(playerName, texture);
                LOGGER.debug("成功加载玩家头像: {}", playerName);
            }
            LOADING.remove(playerName);
        });
    }

    /**
     * 获取玩家头像纹理，支持静态图和 GIF 当前帧。
     */
    public static ResourceLocation getPlayerHeadTexture(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return null;
        }

        Object cached = TEXTURE_CACHE.get(playerName);
        if (cached instanceof ResourceLocation) {
            return (ResourceLocation) cached;
        }
        if (cached instanceof GifAnimatedTexture) {
            return ((GifAnimatedTexture) cached).getCurrentFrame();
        }
        return null;
    }

    /**
     * 判断头像是否为 GIF 动图。
     */
    public static boolean isAnimated(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        Object cached = TEXTURE_CACHE.get(playerName);
        return cached instanceof GifAnimatedTexture && ((GifAnimatedTexture) cached).isAnimated();
    }

    private static Object loadPlayerHeadFromAPI(String playerName) {
        try {
            URL url = new URL(API_URL + playerName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "XUPlus-Client/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream()) {
                    byte[] imageData = inputStream.readAllBytes();
                    if (GifFrameExtractor.isGif(imageData)) {
                        return loadGifHead(playerName, imageData);
                    }
                    return loadStaticHead(playerName, imageData);
                }
            }
            LOGGER.warn("加载玩家头像失败: player={}, HTTP={}", playerName, responseCode);
        } catch (Exception e) {
            LOGGER.error("加载玩家头像异常: {}", playerName, e);
        }
        return null;
    }

    private static ResourceLocation loadStaticHead(String playerName, byte[] imageData) {
        ResourceLocation[] result = new ResourceLocation[1];
        Minecraft.getInstance().execute(() -> {
            try {
                NativeImage nativeImage = NativeImage.read(new java.io.ByteArrayInputStream(imageData));
                DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                result[0] = Minecraft.getInstance()
                    .getTextureManager()
                    .register("xuplus_client_player_head_" + playerName.toLowerCase(), dynamicTexture);
                LOGGER.info("成功加载玩家 {} 的静态头像", playerName);
            } catch (Exception e) {
                LOGGER.error("创建静态头像纹理失败: {}", playerName, e);
            }
        });
        waitForTextureUpload(100L);
        return result[0];
    }

    private static GifAnimatedTexture loadGifHead(String playerName, byte[] imageData) {
        try {
            GifFrameExtractor.GifData gifData = GifFrameExtractor.extractFrames(imageData);
            if (gifData.frames.isEmpty()) {
                return null;
            }

            GifAnimatedTexture[] result = new GifAnimatedTexture[1];
            Minecraft.getInstance().execute(() -> {
                try {
                    List<ResourceLocation> frameTextures = new ArrayList<>();
                    for (int i = 0; i < gifData.frames.size(); i++) {
                        BufferedImage frame = gifData.frames.get(i);
                        NativeImage nativeImage = convertToNativeImage(frame);
                        DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                        ResourceLocation texture = Minecraft.getInstance()
                            .getTextureManager()
                            .register("xuplus_client_player_head_" + playerName.toLowerCase() + "_frame" + i, dynamicTexture);
                        frameTextures.add(texture);
                    }
                    result[0] = new GifAnimatedTexture(frameTextures, gifData.delaysMs);
                    LOGGER.info("成功加载玩家 {} 的 GIF 头像，共 {} 帧", playerName, gifData.frames.size());
                } catch (Exception e) {
                    LOGGER.error("创建 GIF 头像纹理失败: {}", playerName, e);
                }
            });
            waitForTextureUpload(200L);
            return result[0];
        } catch (Exception e) {
            LOGGER.error("解析 GIF 头像失败: {}", playerName, e);
            return null;
        }
    }

    private static NativeImage convertToNativeImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, true);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                nativeImage.setPixelRGBA(x, y, bufferedImage.getRGB(x, y));
            }
        }
        return nativeImage;
    }

    private static void waitForTextureUpload(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("removal")
    private static ResourceLocation getDefaultSkinTexture() {
        return new ResourceLocation("textures/entity/steve.png");
    }

    /**
     * 清除头像缓存。
     */
    public static void clearCache() {
        TEXTURE_CACHE.clear();
        LOADING.clear();
    }

    /**
     * 检查头像纹理是否已经完成加载。
     */
    public static boolean isLoaded(String playerName) {
        if (!TEXTURE_CACHE.containsKey(playerName)) {
            return false;
        }

        Object cached = TEXTURE_CACHE.get(playerName);
        if (cached instanceof ResourceLocation) {
            return Minecraft.getInstance().getTextureManager().getTexture((ResourceLocation) cached) != null;
        }
        if (cached instanceof GifAnimatedTexture) {
            ResourceLocation firstFrame = ((GifAnimatedTexture) cached).getCurrentFrame();
            return firstFrame != null && Minecraft.getInstance().getTextureManager().getTexture(firstFrame) != null;
        }
        return false;
    }
}

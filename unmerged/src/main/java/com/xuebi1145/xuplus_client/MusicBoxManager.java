package com.xuebi1145.xuplus_client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * CS音乐盒管理器 - MVP时播放专属音乐
 */
public class MusicBoxManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("XUPlus-MusicBox");
    private static final String API_URL = "http://t6.sjcmc.cn:35159/music/api/music_api.php?uuid=";
    private static final Gson GSON = new Gson();
    
    private static Clip currentClip = null;
    private static String currentMusicName = "";
    private static boolean isPlaying = false;
    
    /**
     * 当MVP触发时调用 - 查询API并播放音乐
     */
    public static void onMvpTriggered(UUID playerUUID) {
        LOGGER.info("MVP触发，查询音乐: UUID={}", playerUUID);
        CompletableFuture.runAsync(() -> {
            try {
                String urlString = API_URL + playerUUID.toString();
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    LOGGER.warn("音乐API返回错误: HTTP {}", responseCode);
                    return;
                }
                
                // 读取响应
                String response;
                try (InputStream is = conn.getInputStream();
                     java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8")) {
                    scanner.useDelimiter("\\A");
                    response = scanner.hasNext() ? scanner.next() : "";
                }
                
                LOGGER.info("音乐API响应: {}", response);
                
                // 解析JSON
                JsonObject json = GSON.fromJson(response, JsonObject.class);
                if (!json.has("ok") || !json.get("ok").getAsBoolean()) {
                    LOGGER.warn("音乐API返回失败: {}", json.get("error").getAsString());
                    return;
                }
                
                JsonObject data = json.getAsJsonObject("data");
                String musicUrl = data.get("music_url").getAsString();
                String musicName = data.get("music_name").getAsString();
                String coverUrl = data.has("cover_url") ? data.get("cover_url").getAsString() : "";
                String customMemo = data.has("custom_memo") ? data.get("custom_memo").getAsString() : "";
                int vipType = data.has("vip_type") ? data.get("vip_type").getAsInt() : 0;
                int autofish = data.has("autofish") ? data.get("autofish").getAsInt() : 0;
                
                LOGGER.info("获取音乐: {} - {}", musicName, musicUrl);
                
                // 更新玩家完整信息（含封面）
                PlayerVipManager.updateFullInfo(playerUUID, vipType, customMemo, musicName, coverUrl);
                
                // 下载并播放音乐
                playMusicFromUrl(musicUrl, musicName);
                
            } catch (Exception e) {
                LOGGER.error("查询音乐API失败", e);
            }
        });
    }
    
    /**
     * 从URL下载并播放音乐
     */
    private static void playMusicFromUrl(String musicUrl, String musicName) {
        try {
            // 停止当前播放
            stopMusic();
            
            URL url = new URL(musicUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            
            InputStream inputStream = new BufferedInputStream(conn.getInputStream());
            
            // 使用Java Sound API播放
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
            AudioFormat format = audioInputStream.getFormat();
            
            // 转换为PCM格式（兼容性更好）
            AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                format.getSampleRate(),
                16,
                format.getChannels(),
                format.getChannels() * 2,
                format.getSampleRate(),
                false
            );
            
            AudioInputStream decodedAudio = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            
            Clip clip = AudioSystem.getClip();
            clip.open(decodedAudio);
            
            // 应用唱片机/音符盒音量
            applyVolume(clip);
            
            // 添加播放完成监听
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    isPlaying = false;
                    currentMusicName = "";
                    clip.close();
                }
            });
            
            currentClip = clip;
            currentMusicName = musicName;
            isPlaying = true;
            
            clip.start();
            LOGGER.info("开始播放音乐: {}", musicName);
            
        } catch (UnsupportedAudioFileException e) {
            LOGGER.error("不支持的音频格式", e);
        } catch (LineUnavailableException e) {
            LOGGER.error("音频线路不可用", e);
        } catch (IOException e) {
            LOGGER.error("下载音乐失败", e);
        } catch (Exception e) {
            LOGGER.error("播放音乐失败", e);
        }
    }
    
    /**
     * 停止当前音乐
     */
    public static void stopMusic() {
        if (currentClip != null && currentClip.isRunning()) {
            currentClip.stop();
            currentClip.close();
        }
        currentClip = null;
        isPlaying = false;
        currentMusicName = "";
    }
    
    public static boolean isPlaying() {
        return isPlaying;
    }
    
    public static String getCurrentMusicName() {
        return currentMusicName;
    }
    
    /**
     * 每tick调用，实时同步唱片机/音符盒音量
     */
    public static void onClientTick() {
        if (currentClip != null && currentClip.isRunning()) {
            applyVolume(currentClip);
        }
    }
    
    /**
     * 根据Minecraft唱片机/音符盒音量设置调整Clip音量
     */
    private static void applyVolume(Clip clip) {
        try {
            FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            Minecraft mc = Minecraft.getInstance();
            // 获取唱片机/音符盒音量 (0.0 ~ 1.0)
            float mcVolume = mc.options.getSoundSourceVolume(SoundSource.RECORDS);
            // 将0.0~1.0映射到Clip的dB范围
            float minGain = volumeControl.getMinimum();
            float maxGain = volumeControl.getMaximum();
            // 音量为0时静音，否则线性映射到dB
            float gain;
            if (mcVolume <= 0.0f) {
                gain = minGain;
            } else {
                // 线性映射: 0.0->minGain, 1.0->maxGain
                gain = minGain + mcVolume * (maxGain - minGain);
                gain = Math.max(minGain, Math.min(maxGain, gain));
            }
            volumeControl.setValue(gain);
        } catch (Exception e) {
            // 某些音频线路不支持MASTER_GAIN，忽略
        }
    }
}

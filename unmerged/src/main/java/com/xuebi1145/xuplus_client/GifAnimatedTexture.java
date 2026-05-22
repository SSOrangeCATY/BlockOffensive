package com.xuebi1145.xuplus_client;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * GIF动画纹理：管理多帧循环播放
 */
public class GifAnimatedTexture {
    private final List<ResourceLocation> frames;
    private final List<Integer> delays; // 毫秒
    private long lastFrameTimeMs;
    private int currentFrame;

    public GifAnimatedTexture(List<ResourceLocation> frames, List<Integer> delays) {
        this.frames = frames;
        this.delays = delays;
        this.currentFrame = 0;
        this.lastFrameTimeMs = System.currentTimeMillis();
    }

    /**
     * 获取当前帧的ResourceLocation，自动推进帧
     */
    public ResourceLocation getCurrentFrame() {
        if (frames.isEmpty()) return null;
        long now = System.currentTimeMillis();
        int delay = delays.get(currentFrame);
        if (delay > 0 && now - lastFrameTimeMs >= delay) {
            currentFrame = (currentFrame + 1) % frames.size();
            lastFrameTimeMs = now;
        }
        return frames.get(currentFrame);
    }

    public int getFrameCount() {
        return frames.size();
    }

    public boolean isAnimated() {
        return frames.size() > 1;
    }
}

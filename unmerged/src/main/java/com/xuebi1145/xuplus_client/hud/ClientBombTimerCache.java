package com.xuebi1145.xuplus_client.hud;

public final class ClientBombTimerCache {
    private static int fuseTicks;
    private static int totalFuseTicks;
    private static long lastUpdateMs;

    private ClientBombTimerCache() {
    }

    public static void update(int fuse, int totalFuse) {
        fuseTicks = Math.max(0, fuse);
        totalFuseTicks = Math.max(0, totalFuse);
        lastUpdateMs = System.currentTimeMillis();
    }

    public static int fuseTicks() {
        return fuseTicks;
    }

    public static int totalFuseTicks() {
        return totalFuseTicks;
    }

    public static boolean active() {
        return fuseTicks > 0 && System.currentTimeMillis() - lastUpdateMs <= 1500L;
    }

    public static void clear() {
        fuseTicks = 0;
        totalFuseTicks = 0;
        lastUpdateMs = 0L;
    }
}

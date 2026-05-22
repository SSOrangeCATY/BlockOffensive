package com.xuebi1145.xuplus_client.hud;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端投掷物数量缓存
 * 由服务端通过BombItemS2CPacket同步，客户端不直接访问其他玩家背包
 * 
 * 6种投掷物：hegrenade, flashbang, smokegrenade, molotov, incendiary, decoy
 * 最多5个展示位（闪光弹可买2颗）
 */
public class ClientGrenadeCache {
    private static final Map<UUID, int[]> grenadeCountCache = new ConcurrentHashMap<>();

    // 投掷物索引
    public static final int HE = 0;
    public static final int FLASH = 1;
    public static final int SMOKE = 2;
    public static final int MOLOTOV = 3;
    public static final int INCENDIARY = 4;
    public static final int DECOY = 5;
    public static final int COUNT = 6;

    public static void put(UUID uuid, int[] counts) {
        grenadeCountCache.put(uuid, counts);
    }

    public static int[] get(UUID uuid) {
        return grenadeCountCache.getOrDefault(uuid, new int[COUNT]);
    }

    public static void remove(UUID uuid) {
        grenadeCountCache.remove(uuid);
    }

    public static void clear() {
        grenadeCountCache.clear();
    }

    public static Set<UUID> getCacheKeys() {
        return grenadeCountCache.keySet();
    }
}

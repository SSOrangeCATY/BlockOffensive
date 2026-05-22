package com.xuebi1145.xuplus_client.hud;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端C4/剪线钳状态缓存
 * 由服务端通过BombItemS2CPacket同步，客户端不直接访问其他玩家背包
 */
public class ClientBombItemCache {
    private static final Map<UUID, Boolean> hasC4Cache = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> hasDefuserCache = new ConcurrentHashMap<>();

    public static void put(UUID uuid, boolean hasC4, boolean hasDefuser) {
        hasC4Cache.put(uuid, hasC4);
        hasDefuserCache.put(uuid, hasDefuser);
    }

    public static boolean hasC4(UUID uuid) {
        return hasC4Cache.getOrDefault(uuid, false);
    }

    public static boolean hasDefuser(UUID uuid) {
        return hasDefuserCache.getOrDefault(uuid, false);
    }

    public static void remove(UUID uuid) {
        hasC4Cache.remove(uuid);
        hasDefuserCache.remove(uuid);
    }

    public static void clear() {
        hasC4Cache.clear();
        hasDefuserCache.clear();
    }

    public static Set<UUID> getCacheKeys() {
        return hasC4Cache.keySet();
    }
}

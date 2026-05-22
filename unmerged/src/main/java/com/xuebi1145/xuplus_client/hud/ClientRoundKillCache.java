package com.xuebi1145.xuplus_client.hud;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端本回合击杀数缓存
 * 由服务端通过BombItemS2CPacket同步_kills(getTempKills)
 */
public class ClientRoundKillCache {
    private static final Map<UUID, Integer> roundKillCache = new ConcurrentHashMap<>();

    /**
     * 由BombItemS2CPacket调用，直接设置服务端同步的回合击杀数
     */
    public static void putRoundKills(UUID uuid, int kills) {
        roundKillCache.put(uuid, kills);
    }

    /**
     * 获取本回合击杀数
     */
    public static int getRoundKills(UUID uuid) {
        return roundKillCache.getOrDefault(uuid, 0);
    }

    /**
     * 获取缓存中所有UUID（用于retainAll清理离线玩家）
     */
    public static Set<UUID> getCacheKeys() {
        return roundKillCache.keySet();
    }

    /**
     * 清除所有缓存（游戏结束时调用）
     */
    public static void clear() {
        roundKillCache.clear();
    }
}

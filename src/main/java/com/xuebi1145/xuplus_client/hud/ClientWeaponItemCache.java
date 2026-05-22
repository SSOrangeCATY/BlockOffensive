package com.xuebi1145.xuplus_client.hud;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientWeaponItemCache {
    private static final Map<UUID, Map<String, List<ResourceLocation>>> cache = new ConcurrentHashMap<>();

    public static void put(UUID uuid, Map<String, List<ResourceLocation>> itemIds) {
        cache.put(uuid, itemIds);
    }

    public static Map<String, List<ResourceLocation>> get(UUID uuid) {
        return cache.getOrDefault(uuid, Collections.emptyMap());
    }

    public static void clear() {
        cache.clear();
    }

    public static java.util.Set<UUID> getCacheKeys() {
        return cache.keySet();
    }
}

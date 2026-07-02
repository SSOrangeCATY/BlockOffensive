package com.phasetranscrystal.blockoffensive.compat;

import com.phasetranscrystal.blockoffensive.util.BOUtil;
import com.phasetranscrystal.blockoffensive.util.ThrowableType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;

import java.util.Map;

public class CSGrenadeCompat {
    private static final Identifier HE_GRENADE = Identifier.fromNamespaceAndPath("csgrenades", "hegrenade");
    private static final Identifier HE_GRENADE_T = Identifier.fromNamespaceAndPath("csgrenades", "hegrenade_t");
    private static final Identifier INCENDIARY = Identifier.fromNamespaceAndPath("csgrenades", "incendiary");
    private static final Identifier INCENDIARY_T = Identifier.fromNamespaceAndPath("csgrenades", "incendiary_t");
    private static final Identifier MOLOTOV = Identifier.fromNamespaceAndPath("csgrenades", "molotov");
    private static final Identifier MOLOTOV_T = Identifier.fromNamespaceAndPath("csgrenades", "molotov_t");
    private static final Identifier SMOKE_GRENADE = Identifier.fromNamespaceAndPath("csgrenades", "smokegrenade");
    private static final Identifier SMOKE_GRENADE_T = Identifier.fromNamespaceAndPath("csgrenades", "smokegrenade_t");
    private static final Identifier FLASH_BANG = Identifier.fromNamespaceAndPath("csgrenades", "flashbang");
    private static final Identifier FLASH_BANG_T = Identifier.fromNamespaceAndPath("csgrenades", "flashbang_t");
    private static final Identifier DECOY = Identifier.fromNamespaceAndPath("csgrenades", "decoy");
    private static final Identifier DECOY_T = Identifier.fromNamespaceAndPath("csgrenades", "decoy_t");

    public static void registerKillIcon(Map<Identifier, String> registry){
        registerKillIcon(registry, HE_GRENADE, "grenade");
        registerKillIcon(registry, HE_GRENADE_T, "grenade");
        registerKillIcon(registry, INCENDIARY, "ct_incendiary_grenade");
        registerKillIcon(registry, INCENDIARY_T, "ct_incendiary_grenade");
        registerKillIcon(registry, MOLOTOV, "t_incendiary_grenade");
        registerKillIcon(registry, MOLOTOV_T, "t_incendiary_grenade");
        registerKillIcon(registry, SMOKE_GRENADE, "smoke_shell");
        registerKillIcon(registry, SMOKE_GRENADE_T, "smoke_shell");
        registerKillIcon(registry, FLASH_BANG, "flash_bomb");
        registerKillIcon(registry, FLASH_BANG_T, "flash_bomb");
    }

    public static void init() {
        registerThrowable(ThrowableType.FLASH_BANG, FLASH_BANG);
        registerThrowable(ThrowableType.FLASH_BANG, FLASH_BANG_T);
        registerThrowable(ThrowableType.DECOY, DECOY);
        registerThrowable(ThrowableType.DECOY, DECOY_T);
        registerThrowable(ThrowableType.GRENADE, HE_GRENADE);
        registerThrowable(ThrowableType.GRENADE, HE_GRENADE_T);
        registerThrowable(ThrowableType.SMOKE, SMOKE_GRENADE);
        registerThrowable(ThrowableType.SMOKE, SMOKE_GRENADE_T);
        registerThrowable(ThrowableType.INCENDIARY_GRENADE, MOLOTOV);
        registerThrowable(ThrowableType.INCENDIARY_GRENADE, MOLOTOV_T);
        registerThrowable(ThrowableType.INCENDIARY_GRENADE, INCENDIARY);
        registerThrowable(ThrowableType.INCENDIARY_GRENADE, INCENDIARY_T);
    }

    public static boolean is(Entity entity){
        return false;
    }

    private static void registerKillIcon(Map<Identifier, String> registry, Identifier itemId, String iconId) {
        if (BuiltInRegistries.ITEM.containsKey(itemId)) {
            registry.put(itemId, iconId);
        }
    }

    private static void registerThrowable(ThrowableType type, Identifier itemId) {
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (item != null) {
            BOUtil.registerThrowable(type, item);
        }
    }
}

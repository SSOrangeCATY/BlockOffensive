package com.phasetranscrystal.blockoffensive.compat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class LrtacticalCompat {
    public static ItemStack getProjectileItem(DamageSource source) {
        return ItemStack.EMPTY;
    }

    public static boolean itemCheck(Player player) {
        return false;
    }

    public static boolean check(Item item) {
        return false;
    }

    public static boolean isKnife(Item item) {
        return false;
    }

    public static boolean isKnife(ItemStack stack) {
        return false;
    }
}

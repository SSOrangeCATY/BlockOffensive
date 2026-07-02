package com.phasetranscrystal.blockoffensive.compat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CounterStrikeGrenadesCompat {
    public static ItemStack getItemFromDamageSource(DamageSource damageSource) {
        return ItemStack.EMPTY;
    }

    public static boolean itemCheck(Player player) {
        return false;
    }

    public static boolean isPlayerFlashed(Player player) {
        return false;
    }
}

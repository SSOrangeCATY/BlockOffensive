package com.phasetranscrystal.blockoffensive.compat;

import me.xjqsh.lrtactical.api.LrTacticalAPI;
import me.xjqsh.lrtactical.api.item.IMeleeWeapon;
import me.xjqsh.lrtactical.client.resource.display.MeleeDisplayInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;

public class LrtacticalCompat {
    public static boolean isKnife(ItemStack stack) {
        return stack.getItem() instanceof IMeleeWeapon;
    }

    @OnlyIn(Dist.CLIENT)
    public static ResourceLocation getTexture(ItemStack stack) {
        Optional<MeleeDisplayInstance> optional = LrTacticalAPI.getMeleeDisplay(stack);
        if (optional.isPresent()) {
            MeleeDisplayInstance display = optional.get();
            return display.getTexture();
        }
        return null;
    }
}

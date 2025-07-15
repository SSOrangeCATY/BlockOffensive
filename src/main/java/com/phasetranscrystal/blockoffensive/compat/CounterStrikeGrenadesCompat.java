package com.phasetranscrystal.blockoffensive.compat;


import club.pisquad.minecraft.csgrenades.registery.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public class CounterStrikeGrenadesCompat {
    public static void registerKillIcon(Map<ResourceLocation, String> registry){
        ModItems instance = ModItems.INSTANCE;
        registry.put(ForgeRegistries.ITEMS.getKey(instance.getHEGRENADE_ITEM().get()),"grenade");
        registry.put(ForgeRegistries.ITEMS.getKey(instance.getINCENDIARY_ITEM().get()),"ct_incendiary_grenade");
        registry.put(ForgeRegistries.ITEMS.getKey(instance.getMOLOTOV_ITEM().get()),"t_incendiary_grenade");
        registry.put(ForgeRegistries.ITEMS.getKey(instance.getSMOKE_GRENADE_ITEM().get()),"smoke_shell");
        registry.put(ForgeRegistries.ITEMS.getKey(instance.getFLASH_BANG_ITEM().get()),"flash_bomb");
    }
}

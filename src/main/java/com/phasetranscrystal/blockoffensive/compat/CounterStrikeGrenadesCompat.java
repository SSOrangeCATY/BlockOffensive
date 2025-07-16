package com.phasetranscrystal.blockoffensive.compat;


import club.pisquad.minecraft.csgrenades.registery.ModDamageType;
import club.pisquad.minecraft.csgrenades.registery.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    public static ItemStack getItemFromDamageSource(DamageSource damageSource){
        ModDamageType types = ModDamageType.INSTANCE;
        ModItems items = ModItems.INSTANCE;
        Item item;
        if(damageSource.is(types.getFLASHBANG_HIT())){
            item = items.getFLASH_BANG_ITEM().get();
        }else if(damageSource.is(types.getHEGRENADE_HIT())){
            item = items.getHEGRENADE_ITEM().get();
        }else if(damageSource.is(types.getINCENDIARY_HIT())){
            item = items.getINCENDIARY_ITEM().get();
        }else if(damageSource.is(types.getMOLOTOV_HIT())){
            item = items.getMOLOTOV_ITEM().get();
        }else if(damageSource.is(types.getSMOKEGRENADE_HIT())){
            item = items.getSMOKE_GRENADE_ITEM().get();
        }else {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
    }


}

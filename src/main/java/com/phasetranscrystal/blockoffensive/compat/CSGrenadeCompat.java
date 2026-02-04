package com.phasetranscrystal.blockoffensive.compat;

import club.pisquad.minecraft.csgrenades.entity.CounterStrikeGrenadeEntity;
import club.pisquad.minecraft.csgrenades.event.GrenadeThrowEvent;
import club.pisquad.minecraft.csgrenades.registry.ModItems;
import com.phasetranscrystal.blockoffensive.client.BOClientEvent;
import com.phasetranscrystal.blockoffensive.util.BOUtil;
import com.phasetranscrystal.blockoffensive.util.ThrowableType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public class CSGrenadeCompat {

    public static void registerKillIcon(Map<ResourceLocation, String> registry){
        ModItems instance = ModItems.INSTANCE;
        registry.put(ForgeRegistries.ITEMS.getKey(instance.getHEGRENADE_ITEM().get()),"grenade");
        registry.put(ForgeRegistries.ITEMS.getKey(instance.getINCENDIARY_ITEM().get()),"ct_incendiary_grenade");
        registry.put(ForgeRegistries.ITEMS.getKey(instance.getMOLOTOV_ITEM().get()),"t_incendiary_grenade");
        registry.put(ForgeRegistries.ITEMS.getKey(instance.getSMOKE_GRENADE_ITEM().get()),"smoke_shell");
        registry.put(ForgeRegistries.ITEMS.getKey(instance.getFLASH_BANG_ITEM().get()),"flash_bomb");
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(CSGrenadeCompat.class);

        ModItems items = ModItems.INSTANCE;
        BOUtil.registerThrowable(ThrowableType.FLASH_BANG, items.getFLASH_BANG_ITEM().get());
        BOUtil.registerThrowable(ThrowableType.DECOY, items.getDECOY_GRENADE_ITEM().get());
        BOUtil.registerThrowable(ThrowableType.GRENADE, items.getHEGRENADE_ITEM().get());
        BOUtil.registerThrowable(ThrowableType.SMOKE, items.getSMOKE_GRENADE_ITEM().get());
        BOUtil.registerThrowable(ThrowableType.INCENDIARY_GRENADE, items.getMOLOTOV_ITEM().get());
        BOUtil.registerThrowable(ThrowableType.INCENDIARY_GRENADE, items.getINCENDIARY_ITEM().get());
    }

    public static boolean is(Entity entity){
        return entity instanceof CounterStrikeGrenadeEntity;
    }

    @SubscribeEvent
    public static void onThrowGrenadeEvent(GrenadeThrowEvent event) {
        if(BOClientEvent.isLocked()){
            event.setCanceled(true);
        }else{
            BOUtil.buildGrenadeMessageAndSend(event.getItemStack());
        }
    }
}

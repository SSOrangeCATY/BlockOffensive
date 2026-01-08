package com.phasetranscrystal.blockoffensive.compat;

import club.pisquad.minecraft.csgrenades.event.GrenadeThrowEvent;
import com.phasetranscrystal.blockoffensive.client.BOClientEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CSGrenadeCompat {
    public static void init() {
        MinecraftForge.EVENT_BUS.register(CSGrenadeCompat.class);
    }

    @SubscribeEvent
    public static void onThrowGrenadeEvent(GrenadeThrowEvent event) {
        if(BOClientEvent.isLocked()){
            event.setCanceled(true);
        }
    }
}

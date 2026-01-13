package com.phasetranscrystal.blockoffensive.compat;

import club.pisquad.minecraft.csgrenades.event.GrenadeThrowEvent;
import club.pisquad.minecraft.csgrenades.registry.ModItems;
import com.phasetranscrystal.blockoffensive.client.BOClientEvent;
import com.phasetranscrystal.blockoffensive.util.BOUtil;
import com.phasetranscrystal.blockoffensive.util.ThrowableType;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.data.FPSMClientGlobalData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CSGrenadeCompat {
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

    @SubscribeEvent
    public static void onThrowGrenadeEvent(GrenadeThrowEvent event) {
        if(BOClientEvent.isLocked()){
            event.setCanceled(true);
        }else{
            BOClientEvent.buildGrenadeMessageAndSend(event.getItemStack());
        }
    }
}

package com.phasetranscrystal.blockoffensive.map;

import com.phasetranscrystal.blockoffensive.BlockOffensive;

import com.phasetranscrystal.blockoffensive.map.shop.ItemType;
import com.phasetranscrystal.fpsmatch.core.event.register.RegisterFPSMSaveDataEvent;
import com.phasetranscrystal.fpsmatch.core.event.register.RegisterFPSMapEvent;
import com.phasetranscrystal.fpsmatch.core.persistence.SaveHolder;
import com.phasetranscrystal.fpsmatch.core.shop.FPSMShop;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = BlockOffensive.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MapRegister {

    @SubscribeEvent
    public static void onMapRegister(RegisterFPSMapEvent event){
        event.registerGameType("cs", CSGameMap::new);
    }
    @SubscribeEvent
    public static void onDataRegister(RegisterFPSMSaveDataEvent event){
        FPSMShop.registerShopType("cs", ItemType.class);
        event.registerData(CSGameMap.class,"CSGameMaps",
                new SaveHolder.Builder<>(CSGameMap.CODEC)
                        .withReadHandler(CSGameMap::read)
                        .withWriteHandler(CSGameMap::write)
                        .build()
        );
    }
}

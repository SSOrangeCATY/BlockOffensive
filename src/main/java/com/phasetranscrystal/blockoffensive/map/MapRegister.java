package com.phasetranscrystal.blockoffensive.map;

import com.phasetranscrystal.blockoffensive.BlockOffensive;

import com.phasetranscrystal.blockoffensive.data.persistence.CSGameMapFixer;
import com.phasetranscrystal.blockoffensive.map.shop.ItemType;
import com.phasetranscrystal.fpsmatch.common.event.register.RegisterFPSMSaveDataEvent;
import com.phasetranscrystal.fpsmatch.common.event.register.RegisterFPSMapEvent;
import com.phasetranscrystal.fpsmatch.core.persistence.SaveHolder;
import com.phasetranscrystal.fpsmatch.core.persistence.datafixer.DataFixer;
import com.phasetranscrystal.fpsmatch.core.shop.FPSMShop;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = BlockOffensive.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MapRegister {

    @SubscribeEvent
    public static void onMapRegister(RegisterFPSMapEvent event){
        FPSMShop.registerShopType("cs", ItemType.class);
        FPSMShop.registerShopType("csdm", ItemType.class);

        event.registerGameType("cs", CSGameMap::new);
        event.registerGameType("csdm", CSDeathMatchMap::new);
    }

    @SubscribeEvent
    public static void onDataRegister(RegisterFPSMSaveDataEvent event){
        DataFixer.getInstance().registerJsonFixer(CSGameMap.class,0, new CSGameMapFixer.OldV0());

        event.registerData(CSGameMap.class,"CSGameMaps",
                new SaveHolder.Builder<>(CSGameMap.CODEC)
                        .withLoadHandler(CSGameMap::load)
                        .withSaveHandler(CSGameMap::save)
                        .build()
        );

        event.registerData(CSDeathMatchMap.class,"CSDeathMatchMaps",
                new SaveHolder.Builder<>(CSDeathMatchMap.CODEC)
                        .withLoadHandler(CSDeathMatchMap::load)
                        .withSaveHandler(CSDeathMatchMap::save)
                        .build()
        );
    }
}

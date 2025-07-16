package com.phasetranscrystal.blockoffensive.map.shop;

import com.phasetranscrystal.blockoffensive.attributes.BulletproofArmorAttribute;
import com.phasetranscrystal.fpsmatch.core.shop.event.ShopSlotChangeEvent;
import com.phasetranscrystal.fpsmatch.core.shop.functional.ListenerModule;

public class BulletproofArmorWithoutHelmetListenerModule implements ListenerModule {
    @Override
    public void handle(ShopSlotChangeEvent event) {
        if (event.flag >= 1) {
            BulletproofArmorAttribute.addPlayer(event.player,new BulletproofArmorAttribute(false));
        }else{
            BulletproofArmorAttribute.removePlayer(event.player);
        }
    }

    @Override
    public String getName() {
        return "bulletproof_without_helmet";
    }

    @Override
    public int getPriority() {
        return 0;
    }
}

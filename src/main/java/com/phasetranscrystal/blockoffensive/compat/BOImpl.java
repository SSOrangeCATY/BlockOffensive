package com.phasetranscrystal.blockoffensive.compat;

import net.neoforged.fml.ModList;

public class BOImpl {
    public static boolean isGD656KillIconLoaded(){
        return ModList.get().isLoaded("gd656killicon");
    }

    public static boolean isCounterStrikeGrenadesLoaded(){
        return false;
    }

    public static boolean isHitIndicationLoaded(){
        return false;
    }

    public static boolean isPhysicsModLoaded(){
        return false;
    }

    public static boolean isClothConfigLoaded(){
        return ModList.get().isLoaded("cloth_config");
    }

    public static boolean isLrtacticalLoaded(){
        return false;
    }
}

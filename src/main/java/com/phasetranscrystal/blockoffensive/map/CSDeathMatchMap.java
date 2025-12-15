package com.phasetranscrystal.blockoffensive.map;

import com.phasetranscrystal.fpsmatch.common.capability.map.GameEndTeleportCapability;
import com.phasetranscrystal.fpsmatch.common.capability.team.ShopCapability;
import com.phasetranscrystal.fpsmatch.common.capability.team.StartKitsCapability;
import com.phasetranscrystal.fpsmatch.core.capability.map.MapCapability;
import com.phasetranscrystal.fpsmatch.core.capability.team.TeamCapability;
import com.phasetranscrystal.fpsmatch.core.data.AreaData;
import com.phasetranscrystal.fpsmatch.core.data.Setting;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public abstract class CSDeathMatchMap extends CSMap {
    public static final List<Class<? extends MapCapability>> MAP_CAPABILITIES = List.of(GameEndTeleportCapability.class);
    public static final List<Class<? extends TeamCapability>> TEAM_CAPABILITIES = List.of(ShopCapability.class, StartKitsCapability.class);

    public final Setting<Boolean> isPersonal = addSetting("isPersonal", false);

    public CSDeathMatchMap(ServerLevel serverLevel, String mapName, AreaData areaData) {
        super(serverLevel, mapName, areaData,MAP_CAPABILITIES, TEAM_CAPABILITIES);
    }

    @Override
    public void syncToClient() {
        this.getMapTeams().sync();
    }


    @Override
    public boolean start(){
        boolean result = super.start();


        return result;
    }

    @Override
    public void tick(){

    }

    @Override
    public boolean victoryGoal() {
        return false;
    }

    @Override
    public String getGameType() {
        return "csdm";
    }

    @Override
    public CSDeathMatchMap getMap() {
        return this;
    }


}

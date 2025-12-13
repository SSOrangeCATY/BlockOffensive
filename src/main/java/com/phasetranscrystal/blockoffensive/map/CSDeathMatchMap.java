package com.phasetranscrystal.blockoffensive.map;

import com.phasetranscrystal.fpsmatch.common.capability.map.GameEndTeleportCapability;
import com.phasetranscrystal.fpsmatch.common.capability.team.ShopCapability;
import com.phasetranscrystal.fpsmatch.common.capability.team.StartKitsCapability;
import com.phasetranscrystal.fpsmatch.core.capability.map.MapCapability;
import com.phasetranscrystal.fpsmatch.core.capability.team.TeamCapability;
import com.phasetranscrystal.fpsmatch.core.data.AreaData;
import com.phasetranscrystal.fpsmatch.core.data.Setting;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.phasetranscrystal.fpsmatch.core.map.IConfigureMap;
import com.phasetranscrystal.fpsmatch.core.team.TeamData;
import net.minecraft.server.level.ServerLevel;

import java.util.Collection;
import java.util.List;

public class CSDeathMatchMap extends BaseMap implements IConfigureMap<CSDeathMatchMap> {
    public static final List<Class<? extends MapCapability>> MAP_CAPABILITIES = List.of(GameEndTeleportCapability.class);
    public static final List<Class<? extends TeamCapability>> TEAM_CAPABILITIES = List.of(ShopCapability.class, StartKitsCapability.class);

    public boolean isPersonal = true;

    public CSDeathMatchMap(ServerLevel serverLevel, String mapName, AreaData areaData, List<Class<? extends MapCapability>> capabilities) {
        super(serverLevel, mapName, areaData, MAP_CAPABILITIES);

        this.addTeam(TeamData.of("t",16,TEAM_CAPABILITIES));
        this.addTeam(TeamData.of("ct",16,TEAM_CAPABILITIES));
    }

    @Override
    public void syncToClient() {

    }

    @Override
    public boolean victoryGoal() {
        return false;
    }

    @Override
    public String getGameType() {
        return "";
    }

    @Override
    public Collection<Setting<?>> settings() {
        return List.of();
    }

    @Override
    public <I> Setting<I> addSetting(Setting<I> setting) {
        return null;
    }

    @Override
    public CSDeathMatchMap getMap() {
        return this;
    }


}

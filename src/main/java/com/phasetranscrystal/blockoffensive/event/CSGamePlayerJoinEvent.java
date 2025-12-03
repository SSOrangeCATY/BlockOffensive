package com.phasetranscrystal.blockoffensive.event;

import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.team.BaseTeam;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class CSGamePlayerJoinEvent extends Event {
    private final CSGameMap map;
    private final BaseTeam team;
    private final Player player;

    public CSGamePlayerJoinEvent(CSGameMap map, BaseTeam team, Player player) {
        this.map = map;
        this.team = team;
        this.player = player;
    }

    public BaseTeam getTeam() {
        return team;
    }

    public CSGameMap getMap() {
        return map;
    }

    public Player getPlayer() {
        return player;
    }
}

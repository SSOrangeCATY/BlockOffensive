package com.phasetranscrystal.blockoffensive.event;

import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.team.ServerTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public class CSGameMapEvent extends Event {
    private final CSGameMap map;

    public CSGameMapEvent(CSGameMap map) {
        this.map = map;
    }

    public CSGameMap getMap() {
        return map;
    }

    public static class TeamSwitchEvent extends CSGameMapEvent {
        public TeamSwitchEvent(CSGameMap map) {
            super(map);
        }
    }

    public static class PlayerEvent extends CSGameMapEvent {
        public final ServerTeam team;
        public final ServerPlayer player;
        public PlayerEvent(CSGameMap map, ServerTeam team, ServerPlayer player) {
            super(map);
            this.team = team;
            this.player = player;
        }

        public ServerTeam getTeam() {
            return team;
        }

        public ServerPlayer getPlayer() {
            return player;
        }

        public static class PlacedC4Event extends PlayerEvent {
            public PlacedC4Event(CSGameMap map, ServerTeam team, ServerPlayer player) {
                super(map, team, player);
            }
        }
    }
}

package com.xuebi1145.xuplus_client.hud;

import com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity;
import com.phasetranscrystal.blockoffensive.BOConfig;
import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.phasetranscrystal.fpsmatch.core.map.BlastBombState;
import com.xuebi1145.xuplus_client.integrity.IntegrityNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = "xuplus_client", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerBombTimerSync {
    private static int tickCounter;

    private ServerBombTimerSync() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !FPSMCore.initialized()) {
            return;
        }
        if (++tickCounter % 2 != 0) {
            return;
        }
        for (List<BaseMap> maps : FPSMCore.getInstance().getAllMaps().values()) {
            for (BaseMap map : maps) {
                if (map instanceof CSGameMap csMap) {
                    syncMap(csMap);
                }
            }
        }
    }

    private static void syncMap(CSGameMap map) {
        Optional<CompositionC4Entity> c4Opt = findActiveC4(map);
        int fuse = c4Opt.map(CompositionC4Entity::getFuse).filter(value -> value > 0).orElse(0);
        int totalFuse = c4Opt.map(c4 -> Math.max(1, BOConfig.common.fuseTime.get())).orElse(0);
        BombTimerS2CPacket packet = new BombTimerS2CPacket(fuse, totalFuse);
        for (ServerPlayer recipient : map.getMapTeams().getOnlineWithSpec()) {
            IntegrityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), packet);
        }
    }

    private static Optional<CompositionC4Entity> findActiveC4(CSGameMap map) {
        if (!map.isStart() || map.isWaitingWinner() || map.blastState() != BlastBombState.TICKING) {
            return Optional.empty();
        }
        return map.getServerLevel().getEntities(
                EntityTypeTest.forClass(CompositionC4Entity.class),
                c4 -> !c4.isRemoved()
                    && !c4.isDeleting()
                    && c4.getState() == BlastBombState.TICKING
                    && c4.getFuse() > 0
                    && map.getMapArea().aabb().contains(c4.position())
            ).stream()
            .min(Comparator.comparingInt(CompositionC4Entity::getFuse))
            .map(c4 -> (CompositionC4Entity) c4);
    }
}

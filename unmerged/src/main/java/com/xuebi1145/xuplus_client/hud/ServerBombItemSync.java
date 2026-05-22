package com.xuebi1145.xuplus_client.hud;

import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.phasetranscrystal.fpsmatch.core.team.ServerTeam;
import com.xuebi1145.xuplus_client.integrity.IntegrityNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

@Mod.EventBusSubscriber(modid = "xuplus_client", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerBombItemSync {

    private static final String HE_ID = "csgrenades:hegrenade";
    private static final String FLASH_ID = "csgrenades:flashbang";
    private static final String SMOKE_ID = "csgrenades:smokegrenade";
    private static final String MOLOTOV_ID = "csgrenades:molotov";
    private static final String INCENDIARY_ID = "csgrenades:incendiary";
    private static final String DECOY_ID = "csgrenades:decoy";

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            FPSMCore core = FPSMCore.getInstance();
            for (List<BaseMap> maps : core.getAllMaps().values()) {
                for (BaseMap map : maps) {
                    if (!map.isStart()) continue;
                    syncBombItemsForMap(map);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void syncBombItemsForMap(BaseMap map) {
        Map<UUID, boolean[]> bombItemMap = new HashMap<>();
        Map<UUID, int[]> grenadeCountMap = new HashMap<>();
        Map<UUID, Integer> roundKillMap = new HashMap<>();
        List<ServerPlayer> recipients = new ArrayList<>();

        for (ServerTeam team : map.getMapTeams().getNormalTeams()) {
            for (PlayerData pd : team.getPlayers().values()) {
                Optional<ServerPlayer> opt = pd.getPlayer();
                if (opt.isEmpty()) continue;
                ServerPlayer player = opt.get();
                recipients.add(player);

                boolean hasC4 = false;
                boolean hasDefuser = false;
                int[] grenadeCounts = new int[ClientGrenadeCache.COUNT];

                List<List<ItemStack>> items = new ArrayList<>();
                items.add(player.getInventory().items);
                items.add(player.getInventory().armor);
                items.add(player.getInventory().offhand);
                for (List<ItemStack> itemStacks : items) {
                    for (ItemStack itemStack : itemStacks) {
                        if (itemStack.isEmpty()) continue;
                        String regId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
                        if (itemStack.getItem() == BOItemRegister.C4.get()) hasC4 = true;
                        if (itemStack.getItem() == BOItemRegister.BOMB_DISPOSAL_KIT.get()) hasDefuser = true;
                        switch (regId) {
                            case HE_ID -> grenadeCounts[ClientGrenadeCache.HE]++;
                            case FLASH_ID -> grenadeCounts[ClientGrenadeCache.FLASH]++;
                            case SMOKE_ID -> grenadeCounts[ClientGrenadeCache.SMOKE]++;
                            case MOLOTOV_ID -> grenadeCounts[ClientGrenadeCache.MOLOTOV]++;
                            case INCENDIARY_ID -> grenadeCounts[ClientGrenadeCache.INCENDIARY]++;
                            case DECOY_ID -> grenadeCounts[ClientGrenadeCache.DECOY]++;
                        }
                    }
                }
                bombItemMap.put(player.getUUID(), new boolean[]{hasC4, hasDefuser});
                grenadeCountMap.put(player.getUUID(), grenadeCounts);
                roundKillMap.put(player.getUUID(), pd.getTempKills());
            }
        }

        if (bombItemMap.isEmpty()) return;

        BombItemS2CPacket packet = new BombItemS2CPacket(bombItemMap, grenadeCountMap, roundKillMap);
        for (ServerPlayer recipient : recipients) {
            IntegrityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), packet);
        }
    }
}

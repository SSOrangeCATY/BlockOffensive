package com.xuebi1145.xuplus_client.hud;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * S2C包：同步每个玩家的C4/剪线钳持有状态 + 投掷物数量 + 本回合击杀数
 * 服务端检测玩家背包后发送，客户端缓存用于HUD渲染
 */
public record BombItemS2CPacket(Map<UUID, boolean[]> bombItemMap, Map<UUID, int[]> grenadeCountMap, Map<UUID, Integer> roundKillMap) {

    public static void encode(BombItemS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.bombItemMap.size());
        for (Map.Entry<UUID, boolean[]> entry : packet.bombItemMap.entrySet()) {
            buf.writeUUID(entry.getKey());
            boolean[] vals = entry.getValue();
            buf.writeBoolean(vals[0]); // hasC4
            buf.writeBoolean(vals[1]); // hasDefuser
        }
        // 投掷物数量
        buf.writeVarInt(packet.grenadeCountMap.size());
        for (Map.Entry<UUID, int[]> entry : packet.grenadeCountMap.entrySet()) {
            buf.writeUUID(entry.getKey());
            int[] counts = entry.getValue();
            for (int i = 0; i < ClientGrenadeCache.COUNT; i++) {
                buf.writeVarInt(counts[i]);
            }
        }
        // 本回合击杀数
        buf.writeVarInt(packet.roundKillMap.size());
        for (Map.Entry<UUID, Integer> entry : packet.roundKillMap.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    public static BombItemS2CPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<UUID, boolean[]> map = new java.util.HashMap<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            boolean hasC4 = buf.readBoolean();
            boolean hasDefuser = buf.readBoolean();
            map.put(uuid, new boolean[]{hasC4, hasDefuser});
        }
        // 投掷物数量
        int grenadeSize = buf.readVarInt();
        Map<UUID, int[]> grenadeMap = new java.util.HashMap<>(grenadeSize);
        for (int i = 0; i < grenadeSize; i++) {
            UUID uuid = buf.readUUID();
            int[] counts = new int[ClientGrenadeCache.COUNT];
            for (int j = 0; j < ClientGrenadeCache.COUNT; j++) {
                counts[j] = buf.readVarInt();
            }
            grenadeMap.put(uuid, counts);
        }
        // 本回合击杀数
        int killSize = buf.readVarInt();
        Map<UUID, Integer> killMap = new java.util.HashMap<>(killSize);
        for (int i = 0; i < killSize; i++) {
            killMap.put(buf.readUUID(), buf.readVarInt());
        }
        return new BombItemS2CPacket(map, grenadeMap, killMap);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 存入客户端缓存
            for (Map.Entry<UUID, boolean[]> entry : bombItemMap.entrySet()) {
                boolean[] vals = entry.getValue();
                ClientBombItemCache.put(entry.getKey(), vals[0], vals[1]);
            }
            ClientBombItemCache.getCacheKeys().retainAll(bombItemMap.keySet());
            // 投掷物数量缓存
            for (Map.Entry<UUID, int[]> entry : grenadeCountMap.entrySet()) {
                ClientGrenadeCache.put(entry.getKey(), entry.getValue());
            }
            ClientGrenadeCache.getCacheKeys().retainAll(grenadeCountMap.keySet());
            // 本回合击杀数缓存
            for (Map.Entry<UUID, Integer> entry : roundKillMap.entrySet()) {
                ClientRoundKillCache.putRoundKills(entry.getKey(), entry.getValue());
            }
            ClientRoundKillCache.getCacheKeys().retainAll(roundKillMap.keySet());
        });
        context.setPacketHandled(true);
    }
}

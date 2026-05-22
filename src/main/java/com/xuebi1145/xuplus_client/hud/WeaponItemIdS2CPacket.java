package com.xuebi1145.xuplus_client.hud;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public record WeaponItemIdS2CPacket(Map<UUID, Map<String, List<ResourceLocation>>> itemIdMap) {

    public static void encode(WeaponItemIdS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeMap(packet.itemIdMap, FriendlyByteBuf::writeUUID,
                (b, map) -> b.writeMap(map, FriendlyByteBuf::writeUtf,
                        (b1, list) -> b1.writeCollection(list, FriendlyByteBuf::writeResourceLocation)));
    }

    public static WeaponItemIdS2CPacket decode(FriendlyByteBuf buf) {
        Map<UUID, Map<String, List<ResourceLocation>>> itemIdMap = buf.readMap(
                FriendlyByteBuf::readUUID,
                b -> b.readMap(FriendlyByteBuf::readUtf,
                        b1 -> b1.readList(FriendlyByteBuf::readResourceLocation))
        );
        return new WeaponItemIdS2CPacket(itemIdMap);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            for (Map.Entry<UUID, Map<String, List<ResourceLocation>>> entry : itemIdMap.entrySet()) {
                ClientWeaponItemCache.put(entry.getKey(), entry.getValue());
            }
            ClientWeaponItemCache.getCacheKeys().retainAll(itemIdMap.keySet());
        });
        context.setPacketHandled(true);
    }
}

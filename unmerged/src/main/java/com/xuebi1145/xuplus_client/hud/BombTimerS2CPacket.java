package com.xuebi1145.xuplus_client.hud;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BombTimerS2CPacket(int fuseTicks, int totalFuseTicks) {
    public static void encode(BombTimerS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.fuseTicks);
        buf.writeVarInt(packet.totalFuseTicks);
    }

    public static BombTimerS2CPacket decode(FriendlyByteBuf buf) {
        return new BombTimerS2CPacket(buf.readVarInt(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientBombTimerCache.update(fuseTicks, totalFuseTicks));
        context.setPacketHandled(true);
    }
}

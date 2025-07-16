package com.phasetranscrystal.blockoffensive.net;

import com.phasetranscrystal.blockoffensive.compat.PhysicsModCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record PxDeathCompatS2CPacket(UUID uuid) {
    public static void encode(PxDeathCompatS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.uuid);
    }

    public static PxDeathCompatS2CPacket decode(FriendlyByteBuf buf) {
        return new PxDeathCompatS2CPacket(buf.readUUID());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PhysicsModCompat.handleDead(uuid);
        });
        ctx.get().setPacketHandled(true);
    }
}

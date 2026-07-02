package com.phasetranscrystal.blockoffensive.net;

import com.phasetranscrystal.blockoffensive.compat.PhysicsModCompat;
import com.phasetranscrystal.blockoffensive.compat.PhysicsDeathProxyGuard;
import net.minecraft.network.FriendlyByteBuf;
import com.phasetranscrystal.fpsmatch.common.packet.register.NetworkPacketRegister;

import java.util.function.Supplier;

public record PxDeathCompatS2CPacket(int entityId) {

    public static void encode(PxDeathCompatS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
    }

    public static PxDeathCompatS2CPacket decode(FriendlyByteBuf buf) {
        return new PxDeathCompatS2CPacket(buf.readInt());
    }

    public void handle(Supplier<NetworkPacketRegister.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PhysicsModCompat.handleDead(entityId);
        });
        ctx.get().setPacketHandled(true);
    }
}

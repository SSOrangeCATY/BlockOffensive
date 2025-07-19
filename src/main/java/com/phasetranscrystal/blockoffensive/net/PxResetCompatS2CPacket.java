package com.phasetranscrystal.blockoffensive.net;

import com.phasetranscrystal.blockoffensive.compat.PhysicsModCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PxResetCompatS2CPacket {
    public static void encode(PxResetCompatS2CPacket packet, FriendlyByteBuf buf) {
    }

    public static PxResetCompatS2CPacket decode(FriendlyByteBuf buf) {
        return new PxResetCompatS2CPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(PhysicsModCompat::reset);
        ctx.get().setPacketHandled(true);
    }
}

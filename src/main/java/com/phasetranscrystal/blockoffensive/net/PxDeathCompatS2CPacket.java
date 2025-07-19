package com.phasetranscrystal.blockoffensive.net;

import com.phasetranscrystal.blockoffensive.compat.PhysicsModCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PxDeathCompatS2CPacket {
    public static void encode(PxDeathCompatS2CPacket packet, FriendlyByteBuf buf) {
    }

    public static PxDeathCompatS2CPacket decode(FriendlyByteBuf buf) {
        return new PxDeathCompatS2CPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(PhysicsModCompat::handleDead);
        ctx.get().setPacketHandled(true);
    }
}

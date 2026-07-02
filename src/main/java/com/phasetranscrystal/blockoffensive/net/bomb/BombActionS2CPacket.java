package com.phasetranscrystal.blockoffensive.net.bomb;

import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.client.key.DismantleBombKey;

import net.minecraft.network.FriendlyByteBuf;
import com.phasetranscrystal.fpsmatch.common.packet.register.NetworkPacketRegister;

import java.util.function.Supplier;

public record BombActionS2CPacket() {
    public static void encode(BombActionS2CPacket packet, FriendlyByteBuf buf) {
    }

    public static BombActionS2CPacket decode(FriendlyByteBuf buf) {
        return new BombActionS2CPacket();
    }


    public void handle(Supplier<NetworkPacketRegister.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            BlockOffensive.sendToServer(new BombActionC2SPacket(DismantleBombKey.DISMANTLE_BOMB_KEY.isDown()));
        });
        ctx.get().setPacketHandled(true);
    }
}

package com.phasetranscrystal.blockoffensive.net.mvp;

import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameHud;
import com.phasetranscrystal.blockoffensive.client.screen.hud.CSMvpHud;

import com.phasetranscrystal.blockoffensive.data.MvpReason;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import com.phasetranscrystal.fpsmatch.common.packet.register.NetworkPacketRegister;

import java.util.function.Supplier;

public class MvpMessageS2CPacket {
    private final MvpReason mvpReason;

    public MvpMessageS2CPacket(MvpReason mvpReason) {
        this.mvpReason = mvpReason;
    }
    public static void encode(MvpMessageS2CPacket packet, RegistryFriendlyByteBuf buf) {
        buf.writeUUID(packet.mvpReason.uuid);
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, packet.mvpReason.getTeamName());
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, packet.mvpReason.getPlayerName());
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, packet.mvpReason.getMvpReason());
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, packet.mvpReason.getExtraInfo1());
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, packet.mvpReason.getExtraInfo2());
    }

    public static MvpMessageS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new MvpMessageS2CPacket(new MvpReason.Builder(buf.readUUID())
                .setTeamName(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf).copy())
                .setPlayerName(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf).copy())
                .setMvpReason(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf).copy())
                .setExtraInfo1(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf).copy())
                .setExtraInfo2(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf).copy())
                .build());
    }

    public void handle(Supplier<NetworkPacketRegister.Context> ctx) {
        ctx.get().enqueueWork(()-> CSGameHud.getInstance().getMvpHud().triggerAnimation(this.mvpReason));
        ctx.get().setPacketHandled(true);
    }
}

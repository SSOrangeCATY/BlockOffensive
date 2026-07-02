package com.phasetranscrystal.blockoffensive.net;

import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameHud;
import com.phasetranscrystal.blockoffensive.data.DeathMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;
import com.phasetranscrystal.fpsmatch.common.packet.register.NetworkPacketRegister;

import java.util.UUID;
import java.util.function.Supplier;

public class DeathMessageS2CPacket {
    private final DeathMessage deathMessage;

    public DeathMessageS2CPacket(DeathMessage deathMessage) {
        this.deathMessage = deathMessage;
    }

    public static void encode(DeathMessageS2CPacket packet, RegistryFriendlyByteBuf buf) {
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, packet.deathMessage.getKiller());
        buf.writeUUID(packet.deathMessage.getKillerUUID());
        if(packet.deathMessage.getAssist() != null) {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, packet.deathMessage.getAssist());
            buf.writeUUID(packet.deathMessage.getAssistUUID());
        }else{
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, packet.deathMessage.getKiller());
            buf.writeUUID(packet.deathMessage.getKillerUUID());
        }
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, packet.deathMessage.getDead());
        buf.writeUUID(packet.deathMessage.getDeadUUID());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.deathMessage.getWeapon());
        buf.writeUtf(packet.deathMessage.getArg());
        
        byte flags = 0;
        flags |= (byte) (packet.deathMessage.isHeadShot() ? 1 : 0);
        flags |= (byte) (packet.deathMessage.isBlinded() ? 2 : 0);
        flags |= (byte) (packet.deathMessage.isThroughSmoke() ? 4 : 0);
        flags |= (byte) (packet.deathMessage.isThroughWall() ? 8 : 0);
        flags |= (byte) (packet.deathMessage.isNoScope() ? 16 : 0);
        flags |= (byte) (packet.deathMessage.isFlying() ? 32 : 0);
        buf.writeByte(flags);
    }

    public static DeathMessageS2CPacket decode(RegistryFriendlyByteBuf buf) {
        Component killer = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
        UUID killerUUID = buf.readUUID();
        Component assist = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
        UUID assistUUID = buf.readUUID();
        Component dead = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
        UUID deadUUID = buf.readUUID();
        ItemStack weapon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        String arg = buf.readUtf();
        byte flags = buf.readByte();
        
        return new DeathMessageS2CPacket(new DeathMessage.Builder(killer, killerUUID, dead, deadUUID, weapon)
            .setAssist(assist, assistUUID)
            .setArg(arg)
            .setHeadShot((flags & 1) != 0)
            .setBlinded((flags & 2) != 0)
            .setThroughSmoke((flags & 4) != 0)
            .setThroughWall((flags & 8) != 0)
            .setNoScope((flags & 16) != 0)
            .setFlying((flags & 32) != 0)
            .build());
    }

    public void handle(Supplier<NetworkPacketRegister.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CSGameHud.getInstance().getDeathMessageHud().addKillMessage(deathMessage);
            boolean isLocalKill = Minecraft.getInstance().player != null &&
                    deathMessage.getKillerUUID().equals(Minecraft.getInstance().player.getUUID());
            boolean isLocalDead = deathMessage.getDeadUUID().equals(Minecraft.getInstance().player.getUUID());
            if(isLocalKill && !isLocalDead) {
                CSGameHud.getInstance().addKill(deathMessage);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

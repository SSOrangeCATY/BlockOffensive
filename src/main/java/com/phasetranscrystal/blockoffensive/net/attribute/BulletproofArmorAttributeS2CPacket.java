package com.phasetranscrystal.blockoffensive.net.attribute;

import com.phasetranscrystal.blockoffensive.attributes.BulletproofArmorAttribute;
import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BulletproofArmorAttributeS2CPacket {
    private final boolean hasHelmet;
    private final int durability;

    public BulletproofArmorAttributeS2CPacket(BulletproofArmorAttribute attribute) {
        this(attribute.hasHelmet(), attribute.getDurability());
    }

    public BulletproofArmorAttributeS2CPacket(boolean hasHelmet, int durability) {
        this.hasHelmet = hasHelmet;
        this.durability = durability;
    }

    public static void encode(BulletproofArmorAttributeS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.hasHelmet);
        buf.writeInt(packet.durability);
    }

    public static BulletproofArmorAttributeS2CPacket decode(FriendlyByteBuf buf) {
        return new BulletproofArmorAttributeS2CPacket(buf.readBoolean(),buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CSClientData.bpAttributeDurability = durability;
            CSClientData.bpAttributeHasHelmet = hasHelmet;
        });
        ctx.get().setPacketHandled(true);
    }
}

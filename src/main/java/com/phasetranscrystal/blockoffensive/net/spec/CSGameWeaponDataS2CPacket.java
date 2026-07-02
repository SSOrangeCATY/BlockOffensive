package com.phasetranscrystal.blockoffensive.net.spec;

import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.data.WeaponData;
import net.minecraft.network.FriendlyByteBuf;
import com.phasetranscrystal.fpsmatch.common.packet.register.NetworkPacketRegister;

import java.util.*;
import java.util.function.Supplier;

public record CSGameWeaponDataS2CPacket(Map<UUID, WeaponData> weaponDataMap) {
    public static void encode(CSGameWeaponDataS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeMap(packet.weaponDataMap, (b, uuid) -> b.writeUUID(uuid),
                (b, weaponData) -> {
                    b.writeMap(weaponData.weaponData(), (b1, value) -> b1.writeUtf(value),
                            (b1, list) -> b1.writeCollection(list, (b2, value) -> b2.writeUtf(value)));
                    b.writeBoolean(weaponData.bpAttributeHasHelmet());
                    b.writeInt(weaponData.bpAttributeDurability());
                });
    }

    public static CSGameWeaponDataS2CPacket decode(FriendlyByteBuf buf) {
        Map<UUID, WeaponData> weaponDataMap = buf.readMap(
                b -> b.readUUID(),
                b -> {
                    Map<String, List<String>> weaponData = b.readMap(
                            b1 -> b1.readUtf(),
                            b1 -> b1.readList(b2 -> b2.readUtf())
                    );
                    boolean hasHelmet = b.readBoolean();
                    int durability = b.readInt();

                    return new WeaponData(weaponData, hasHelmet, durability);
                }
        );
        return new CSGameWeaponDataS2CPacket(weaponDataMap);
    }

    public void handle(Supplier<NetworkPacketRegister.Context> contextSupplier) {
        NetworkPacketRegister.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            synchronized (CSClientData.weaponData) {
                CSClientData.weaponData.putAll(weaponDataMap);

                Set<UUID> keysToRemove = new HashSet<>();
                for (UUID uuid : CSClientData.weaponData.keySet()) {
                    if (!weaponDataMap.containsKey(uuid)) {
                        keysToRemove.add(uuid);
                    }
                }
                for (UUID uuid : keysToRemove) {
                    CSClientData.weaponData.remove(uuid);
                }
            }
        });
        context.setPacketHandled(true);
    }
}

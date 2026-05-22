package com.xuebi1145.xuplus_client.mixin;

import com.phasetranscrystal.blockoffensive.client.data.WeaponData;
import com.phasetranscrystal.blockoffensive.map.CSMap;
import com.phasetranscrystal.blockoffensive.net.spec.CSGameWeaponDataS2CPacket;
import com.phasetranscrystal.fpsmatch.common.attributes.ammo.BulletproofArmorAttribute;
import com.phasetranscrystal.fpsmatch.common.drop.DropType;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.team.ServerTeam;
import com.xuebi1145.xuplus_client.hud.WeaponItemIdS2CPacket;
import com.xuebi1145.xuplus_client.integrity.IntegrityNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

/**
 * 淇敼CSMap.syncWeaponData锛屽湪鍘熸湁鍙戦€佺粰鏃佽鑰呯殑鍩虹涓婏紝
 * 棰濆鍙戦€佹鍣ㄦ暟鎹粰鎵€鏈夐槦鍙嬶紝浣块槦鍙嬭兘鐪嬪埌褰兼鐨勬鍣ㄤ俊鎭? * C4/鍓嚎閽?鎶曟幏鐗╁悓姝ョ敱ServerBombItemSync鐙珛澶勭悊
 */
@Mixin(value = CSMap.class, remap = false)
public class MixinCSMapSyncWeapon {

    @Inject(method = "syncWeaponData", at = @At("TAIL"), remap = false)
    private void xuplus$syncWeaponDataToTeammates(CallbackInfo ci) {
        CSMap self = (CSMap) (Object) this;

        // 鏋勫缓姝﹀櫒鏁版嵁
        Map<UUID, WeaponData> weaponDataMap = new HashMap<>();
        Map<UUID, Map<String, List<ResourceLocation>>> itemIdMap = new HashMap<>();

        for (PlayerData data : self.getMapTeams().getJoinedPlayers()) {
            Optional<ServerPlayer> optional = data.getPlayer();
            if (optional.isEmpty()) continue;
            ServerPlayer player = optional.get();
            Map<String, List<String>> weaponData = new HashMap<>();
            Map<String, List<ResourceLocation>> itemIds = new HashMap<>();

            List<List<ItemStack>> items = new ArrayList<>();
            items.add(player.getInventory().items);
            items.add(player.getInventory().armor);
            items.add(player.getInventory().offhand);
            for (List<ItemStack> itemStacks : items) {
                for (ItemStack itemStack : itemStacks) {
                    if (itemStack.isEmpty()) continue;
                    ResourceLocation regId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(itemStack.getItem());
                    for (DropType dropType : DropType.values()) {
                        if (dropType.itemMatch().test(itemStack)) {
                            weaponData.computeIfAbsent(dropType.name(), k -> new ArrayList<>()).add(itemStack.getHoverName().getString());
                            if (itemStack.getItem() instanceof com.tacz.guns.api.item.IGun iGun) {
                                itemIds.computeIfAbsent(dropType.name(), k -> new ArrayList<>()).add(iGun.getGunId(itemStack));
                            } else {
                                itemIds.computeIfAbsent(dropType.name(), k -> new ArrayList<>()).add(regId);
                            }
                            break;
                        }
                    }
                }
            }
            weaponData.computeIfAbsent("CARRIED", k -> new ArrayList<>()).add(player.getMainHandItem().getHoverName().getString());
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof com.tacz.guns.api.item.IGun iGun) {
                itemIds.computeIfAbsent("CARRIED", k -> new ArrayList<>()).add(iGun.getGunId(mainHand));
            } else {
                ResourceLocation carriedId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(mainHand.getItem());
                itemIds.computeIfAbsent("CARRIED", k -> new ArrayList<>()).add(carriedId);
            }

            boolean hasHelmet;
            int durability;
            Optional<BulletproofArmorAttribute> attribute = BulletproofArmorAttribute.getInstance(player);
            if (attribute.isPresent()) {
                hasHelmet = attribute.get().hasHelmet();
                durability = attribute.get().getDurability();
            } else {
                hasHelmet = false;
                durability = 0;
            }
            weaponDataMap.put(player.getUUID(), new WeaponData(weaponData, hasHelmet, durability));
            itemIdMap.put(player.getUUID(), itemIds);
        }

        CSGameWeaponDataS2CPacket packet = new CSGameWeaponDataS2CPacket(weaponDataMap);
        WeaponItemIdS2CPacket itemIdPacket = new WeaponItemIdS2CPacket(itemIdMap);

        // 鍙戦€佺粰鎵€鏈夐潪鏃佽鑰呯帺瀹讹紙闃熷弸鍙互鐪嬪埌褰兼鐨勬鍣級
        for (ServerTeam team : self.getMapTeams().getNormalTeams()) {
            for (PlayerData pd : team.getPlayers().values()) {
                pd.getPlayer().ifPresent(player -> {
                    self.sendPacketToJoinedPlayer(player, packet, true);
                    IntegrityNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), itemIdPacket);
                });
            }
        }
    }
}

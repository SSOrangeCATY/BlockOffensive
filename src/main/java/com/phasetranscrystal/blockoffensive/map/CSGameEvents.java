package com.phasetranscrystal.blockoffensive.map;

import com.phasetranscrystal.blockoffensive.compat.BOImpl;
import com.phasetranscrystal.blockoffensive.data.DeathMessage;
import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.blockoffensive.item.BombDisposalKit;
import com.phasetranscrystal.blockoffensive.item.CompositionC4;
import com.phasetranscrystal.blockoffensive.net.DeathMessageS2CPacket;
import com.phasetranscrystal.blockoffensive.net.PxDeathCompatS2CPacket;
import com.phasetranscrystal.fpsmatch.common.packet.FPSMatchRespawnS2CPacket;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CSGameEvents {

    @SubscribeEvent
    public static void onPlayerLoggedOutEvent(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
            Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(player);
            if(optional.isEmpty()) return;
            BaseMap map = optional.get();
            if(map instanceof CSMap){
                CSMap.dropC4(player);
                player.getInventory().clearContent();
            }
        }
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event){
        Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(event.getPlayer());
        if(optional.isEmpty()) return;
        BaseMap map = optional.get();
        if(map instanceof CSGameMap csGameMap){
            String[] m = event.getMessage().getString().split("\\.");
            if(m.length > 1){
                csGameMap.handleChatCommand(m[1],event.getPlayer());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDropItem(ItemTossEvent event){
        if(event.getEntity().level().isClientSide) return;
        ItemStack itemStack = event.getEntity().getItem();
        Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(event.getPlayer());
        if(optional.isEmpty()) return;
        BaseMap map = optional.get();
        if (map instanceof CSGameMap){
            if(itemStack.getItem() instanceof CompositionC4){
                event.getEntity().setGlowingTag(true);
            }

            if(itemStack.getItem() instanceof BombDisposalKit){
                event.setCanceled(true);
                event.getPlayer().displayClientMessage(Component.translatable("blockoffensive.item.bomb_disposal_kit.drop.message").withStyle(ChatFormatting.RED),true);
                event.getPlayer().getInventory().add(new ItemStack(BOItemRegister.BOMB_DISPOSAL_KIT.get(),1));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerKilledByGun(EntityKillByGunEvent event){
        if(event.getLogicalSide() == LogicalSide.SERVER){
            if (event.getKilledEntity() instanceof ServerPlayer player) {
                Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(player);
                if(optional.isEmpty()) return;
                BaseMap map = optional.get();
                if (map instanceof CSGameMap cs && cs.checkGameHasPlayer(player)) {
                    if(event.getAttacker() instanceof ServerPlayer attacker){
                        Optional<BaseMap> optionalFromMap = FPSMCore.getInstance().getMapByPlayer(player);
                        if(optionalFromMap.isEmpty()) return;
                        BaseMap fromMap = optionalFromMap.get();
                        if (fromMap instanceof CSGameMap csGameMap && csGameMap.equals(map)) {
                            if(IGun.mainHandHoldGun(attacker)) {
                                csGameMap.giveEco(player,attacker,attacker.getMainHandItem(),true);
                                csGameMap.getMapTeams().getTeamByPlayer(attacker).ifPresent(team->{
                                    if(event.isHeadShot()){
                                        team.getPlayerData(attacker.getUUID()).ifPresent(PlayerData::addHeadshotKill);
                                    }
                                });

                                DeathMessage.Builder builder = new DeathMessage.Builder(attacker, player, attacker.getMainHandItem()).setHeadShot(event.isHeadShot());
                                Map<UUID, Float> hurtDataMap = cs.getMapTeams().getDamageMap().get(player.getUUID());
                                if (hurtDataMap != null && !hurtDataMap.isEmpty()) {
                                    hurtDataMap.entrySet().stream()
                                            .filter(entry -> entry.getValue() > player.getMaxHealth() / 4)
                                            .sorted(Map.Entry.<UUID, Float>comparingByValue().reversed())
                                            .limit(1)
                                            .findAny()
                                            .flatMap(entry -> cs.getMapTeams().getTeamByPlayer(entry.getKey())
                                                    .flatMap(team -> team.getPlayerData(entry.getKey()))).ifPresent(playerData -> {
                                                if (!attacker.getUUID().equals(playerData.getOwner())){
                                                    builder.setAssist(playerData.name(), playerData.getOwner());
                                                }
                                            });
                                }
                                DeathMessageS2CPacket killMessageS2CPacket = new DeathMessageS2CPacket(builder.build());
                                csGameMap.sendPacketToAllPlayer(killMessageS2CPacket);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 玩家死亡事件处理
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeathEvent(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(player);
            if (optional.isPresent() && optional.get() instanceof CSGameMap csGameMap) {
                if(BOImpl.isPhysicsModLoaded()){
                    csGameMap.sendPacketToAllPlayer(new PxDeathCompatS2CPacket(player.getId()));
                }
                csGameMap.onPlayerDeathEvent(player,event.getSource());
                csGameMap.sendPacketToJoinedPlayer(player,new FPSMatchRespawnS2CPacket(),true);
                event.setCanceled(true);
            }
        }
    }


}

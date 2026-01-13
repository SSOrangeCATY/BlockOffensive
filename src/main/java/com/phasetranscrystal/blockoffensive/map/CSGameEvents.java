package com.phasetranscrystal.blockoffensive.map;

import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.compat.BOImpl;
import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.blockoffensive.item.BombDisposalKit;
import com.phasetranscrystal.blockoffensive.item.CompositionC4;
import com.phasetranscrystal.blockoffensive.net.PxDeathCompatS2CPacket;
import com.phasetranscrystal.blockoffensive.util.BOUtil;
import com.phasetranscrystal.fpsmatch.common.packet.FPSMatchRespawnS2CPacket;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.common.event.FPSMapEvent;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.phasetranscrystal.fpsmatch.util.FPSMUtil;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = BlockOffensive.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CSGameEvents {
    @SubscribeEvent
    public static void onPlayerHurt(FPSMapEvent.PlayerEvent.HurtEvent event) {
        if (event.getMap() instanceof CSDeathMatchMap dm){
            if(dm.isInSpawnProtection(event.getPlayer().getUUID())){
                event.setCanceled(true);
            }else{
               boolean isTeammate = FPSMUtil.getAttackerFromDamageSource(event.getSource())
                       .map(attacker -> dm.getMapTeams().isSameTeam(event.getPlayer(), attacker))
                       .orElse(false);

               if(dm.isTDM() && isTeammate){
                   event.setCanceled(true);
               }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerShoot(GunShootEvent event) {
        if (event.getLogicalSide() == LogicalSide.CLIENT) return;

        if(event.getShooter() instanceof Player player) {
            FPSMCore.getInstance().getMapByPlayer(player)
                    .map(map->{
                        if(map instanceof CSDeathMatchMap dm){
                            return dm;
                        }
                        return null;
                    }).ifPresent(dm->{
                        dm.handlePlayerFire(player.getUUID());
                    });
        }
    }

    // 在登出时自动清理身上的C4和物品
    @SubscribeEvent
    public static void onPlayerLoggedOutEvent(FPSMapEvent.PlayerEvent.LoggedOutEvent event){
        if(event.getMap() instanceof CSMap){
            ServerPlayer player = event.getPlayer();
            CSMap.dropC4(player);
            player.getInventory().clearContent();
            event.setCanceled(true);
        }
    }

    //处理地图命令
    @SubscribeEvent
    public static void onChat(FPSMapEvent.PlayerEvent.ChatEvent event){
        if(event.getMap() instanceof CSMap csGameMap){
            String[] m = event.getMessage().split("\\.");
            if(m.length > 1){
                csGameMap.handleChatCommand(m[1],event.getPlayer());
            }
        }
    }

    //控制地图物品掉落
    @SubscribeEvent
    public static void onPlayerDropItem(FPSMapEvent.PlayerEvent.TossItemEvent event){
        ServerPlayer player = event.getPlayer();
        ItemStack itemStack = event.getItemEntity().getItem();
        BaseMap map = event.getMap();
        if (map instanceof CSMap cs){
            if( cs instanceof CSDeathMatchMap){
                event.setCanceled(true);
            }

            if(itemStack.getItem() instanceof BombDisposalKit){
                event.setCanceled(true);
                event.getPlayer().getInventory().add(new ItemStack(BOItemRegister.BOMB_DISPOSAL_KIT.get(),1));
            }

            if(itemStack.getItem() instanceof CompositionC4){
                event.getItemEntity().setGlowingTag(true);
            }

            if(!event.isCanceled()){
                FPSMUtil.sortPlayerInventory(player);
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerKilledByGun(EntityKillByGunEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER || !(event.getKilledEntity() instanceof ServerPlayer deadPlayer)) {
            return;
        }

        Optional<CSMap> csMapOpt = FPSMCore.getInstance().getMapByPlayer(deadPlayer)
                .filter(map -> map instanceof CSMap)
                .map(map -> (CSMap) map);
        if (csMapOpt.isEmpty()) {
            return;
        }
        CSMap csMap = csMapOpt.get();

        if (!(event.getAttacker() instanceof ServerPlayer attacker) || !IGun.mainHandHoldGun(attacker)) {
            return;
        }

        if (!FPSMCore.getInstance().getMapByPlayer(attacker).map(map -> map.equals(csMap)).orElse(false)) {
            return;
        }

        ItemStack deathItem = attacker.getMainHandItem();
        csMap.onPlayerDeathEvent(deadPlayer, attacker, deathItem, event.isHeadShot());
    }

    /**
     * 玩家死亡事件处理
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeathEvent(FPSMapEvent.PlayerEvent.DeathEvent event) {
        if(!(event.getMap() instanceof CSMap csMap)) return;

        ServerPlayer player = event.getPlayer();

        if (BOImpl.isPhysicsModLoaded()) {
            csMap.sendPacketToAllPlayer(new PxDeathCompatS2CPacket(player.getId()));
        }

        Optional<ServerPlayer> attackerOpt = event.getKiller();
        ItemStack deathItem = attackerOpt.map(attacker -> BOUtil.getDeathItemStack(attacker, event.getSource()))
                .orElse(ItemStack.EMPTY);

        csMap.onPlayerDeathEvent(player, attackerOpt.orElse(null), deathItem, false);

        csMap.sendPacketToJoinedPlayer(player, new FPSMatchRespawnS2CPacket(), true);
        event.setCanceled(true);
    }

}

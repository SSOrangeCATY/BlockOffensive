package com.phasetranscrystal.blockoffensive.map;

import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.compat.BOImpl;
import com.phasetranscrystal.blockoffensive.data.DeathMessage;
import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.blockoffensive.item.BombDisposalKit;
import com.phasetranscrystal.blockoffensive.item.CompositionC4;
import com.phasetranscrystal.blockoffensive.net.DeathMessageS2CPacket;
import com.phasetranscrystal.blockoffensive.net.PxDeathCompatS2CPacket;
import com.phasetranscrystal.blockoffensive.util.BOUtil;
import com.phasetranscrystal.fpsmatch.common.packet.FPSMatchRespawnS2CPacket;
import com.phasetranscrystal.fpsmatch.compat.CounterStrikeGrenadesCompat;
import com.phasetranscrystal.fpsmatch.compat.impl.FPSMImpl;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlockOffensive.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CSGameEvents {

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) {
            FPSMCore.getInstance().getMapByPlayer(player)
                    .map(map->{
                        if(map instanceof CSDeathMatchMap dm){
                            return dm;
                        }
                        return null;
                    }).ifPresent(dm->{
                        if(dm.isInSpawnProtection(player.getUUID())){
                            event.setCanceled(true);
                        }
                    });
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

    //处理地图命令
    @SubscribeEvent
    public static void onChat(ServerChatEvent event){
        Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(event.getPlayer());
        if(optional.isEmpty()) return;
        BaseMap map = optional.get();
        if(map instanceof CSMap csGameMap){
            String[] m = event.getMessage().getString().split("\\.");
            if(m.length > 1){
                csGameMap.handleChatCommand(m[1],event.getPlayer());
            }
        }
    }

    //控制地图物品掉落
    @SubscribeEvent
    public static void onPlayerDropItem(ItemTossEvent event){
        if(event.getEntity().level().isClientSide) return;
        ItemStack itemStack = event.getEntity().getItem();
        Optional<BaseMap> optional = FPSMCore.getInstance().getMapByPlayer(event.getPlayer());
        if(optional.isEmpty()) return;
        BaseMap map = optional.get();
        if (map instanceof CSMap cs){
            if(cs instanceof CSDeathMatchMap){
                event.setCanceled(true);
            }

            if(itemStack.getItem() instanceof CompositionC4){
                event.getEntity().setGlowingTag(true);
            }

            if(itemStack.getItem() instanceof BombDisposalKit){
                event.setCanceled(true);
                event.getPlayer().getInventory().add(new ItemStack(BOItemRegister.BOMB_DISPOSAL_KIT.get(),1));
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerKilledByGun(EntityKillByGunEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER || !(event.getKilledEntity() instanceof ServerPlayer deadPlayer)) {
            return;
        }

        // 获取死亡玩家所在的CS地图
        Optional<CSMap> csMapOpt = FPSMCore.getInstance().getMapByPlayer(deadPlayer)
                .filter(map -> map instanceof CSMap)
                .map(map -> (CSMap) map);
        if (csMapOpt.isEmpty()) {
            return;
        }
        CSMap csMap = csMapOpt.get();

        // 验证击杀者是玩家且持有枪械
        if (!(event.getAttacker() instanceof ServerPlayer attacker) || !IGun.mainHandHoldGun(attacker)) {
            return;
        }

        // 验证击杀者和死亡玩家在同一地图
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
    public static void onLivingDeathEvent(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) {
            return;
        }

        // 获取CS地图并验证
        Optional<CSMap> csMapOpt = FPSMCore.getInstance().getMapByPlayer(deadPlayer)
                .filter(map -> map instanceof CSMap)
                .map(map -> (CSMap) map);
        if (csMapOpt.isEmpty()) {
            return;
        }

        CSMap csMap = csMapOpt.get();

        if (BOImpl.isPhysicsModLoaded()) {
            csMap.sendPacketToAllPlayer(new PxDeathCompatS2CPacket(deadPlayer.getId()));
        }

        Optional<ServerPlayer> attackerOpt = BOUtil.getAttackerFromDamageSource(event.getSource());
        ItemStack deathItem = attackerOpt.map(attacker -> BOUtil.getDeathItemStack(attacker, event.getSource()))
                .orElse(ItemStack.EMPTY);

        // 处理核心死亡逻辑
        csMap.onPlayerDeathEvent(deadPlayer, attackerOpt.orElse(null), deathItem, false);

        csMap.sendPacketToJoinedPlayer(deadPlayer, new FPSMatchRespawnS2CPacket(), true);
        event.setCanceled(true);
    }

}

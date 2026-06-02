package com.phasetranscrystal.blockoffensive.map;

import com.phasetranscrystal.blockoffensive.event.CSGameMapEvent;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.blockoffensive.item.BombDisposalKit;
import com.phasetranscrystal.blockoffensive.item.CompositionC4;
import com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity;
import com.phasetranscrystal.fpsmatch.common.attributes.ammo.BulletproofArmorAttribute;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.common.event.FPSMapEvent;
import com.phasetranscrystal.fpsmatch.common.event.PlayerObtainItemEvent;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import com.phasetranscrystal.fpsmatch.util.FPSMUtil;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = BlockOffensive.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CSGameEvents {

    @SubscribeEvent
    public static void onPlayerHurt(FPSMapEvent.PlayerEvent.HurtEvent event) {
        BaseMap map = event.getMap();

        if(!(map instanceof CSMap cs)) return;
        Optional<ServerPlayer> opt = event.getMap().getAttackerFromDamageSource(event.getSource());
        boolean isTeammate = opt.map(attacker -> cs.getMapTeams().isSameTeam(event.getPlayer(), attacker)).orElse(false);

        if (cs instanceof CSDeathMatchMap dm){
            if(dm.isInSpawnProtection(event.getPlayer().getUUID())){
                event.setCanceled(true);
            }else{
               if(dm.isTDM() && isTeammate){
                   event.setCanceled(true);
               }
            }
        }else{
            if(isTeammate){
                if (isC4Kill(event.getSource())) {
                    return;
                }
                if(cs.allowFriendlyFire()){
                    event.setAmount(event.getAmount() * 0.3F);
                    cs.handleTeammateAttack(opt.get(),event.getPlayer());
                }else{
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onKillRecord(FPSMapEvent.PlayerEvent.KillRecordEvent event) {
        if (event.getMap() instanceof CSMap && isC4Kill(event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTeamKillPenalty(FPSMapEvent.PlayerEvent.KillEvent event) {
        if (!(event.getMap() instanceof CSMap cs)) return;

        ServerPlayer killer = event.getPlayer();
        ServerPlayer dead = event.getDead();
        if (cs.getMapTeams().isSameTeam(killer, dead) && !isC4Kill(event.getSource())) {
            cs.getMapTeams().getPlayerData(killer).ifPresent(PlayerData::removeKill);
        }
    }

    private static boolean isC4Kill(DamageSource source) {
        return source.getDirectEntity() instanceof CompositionC4Entity;
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
                    }).ifPresent(dm-> dm.handlePlayerFire(player.getUUID()));
        }
    }

    // 在登出时自动清理身上的C4和物品
    @SubscribeEvent
    public static void onPlayerLoggedOutEvent(FPSMapEvent.PlayerEvent.LoggedOutEvent event){
        if(event.getMap() instanceof CSMap){
            ServerPlayer player = event.getPlayer();
            CSMap.dropC4(player);
            player.getInventory().clearContent();
            BulletproofArmorAttribute.removePlayer(player);
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

    /**
     * 队伍换边事件处理 - 移除所有玩家的防弹衣属性
     */
    @SubscribeEvent
    public static void onTeamSwitch(CSGameMapEvent.TeamSwitchEvent event) {
        event.getMap().getMapTeams().getJoinedPlayers().forEach(data ->
            data.getPlayer().ifPresent(BulletproofArmorAttribute::removePlayer)
        );
    }

    @SubscribeEvent
    public static void onGunReload(GunReloadEvent event) {
        if (event.getLogicalSide() == LogicalSide.CLIENT) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        FPSMCore.getInstance().getMapByPlayer(player)
                .filter(map -> map instanceof CSGameMap)
                .map(map -> (CSGameMap) map)
                .filter(CSGameMap::isMagazineMode)
                .ifPresent(cs -> {
                    ItemStack stack = event.getGunItemStack();
                    if (stack != null && stack.getItem() instanceof IGun iGun) {
                        applyMagazineReload(stack, iGun);
                    }
                });
    }

    @SubscribeEvent
    public static void onPlayerObtainItem(PlayerObtainItemEvent event) {
        if (!(event.getMap() instanceof CSGameMap cs) || !cs.isMagazineMode()) return;
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof IGun iGun) {
            applyMagazineObtainAmmo(stack, iGun);
        }
    }

    private static void applyMagazineReload(ItemStack stack, IGun iGun) {
        int dummyAmmo = iGun.getDummyAmmoAmount(stack);
        int maxAmmo = TimelessAPI.getCommonGunIndex(iGun.getGunId(stack))
                .map(gunIndex -> gunIndex.getGunData().getAmmoAmount())
                .orElse(0);
        if (maxAmmo <= 0 || dummyAmmo < maxAmmo) return;

        int magazineCount = dummyAmmo / maxAmmo;
        iGun.setDummyAmmoAmount(stack, (magazineCount - 1) * maxAmmo);
    }

    private static void applyMagazineObtainAmmo(ItemStack stack, IGun iGun) {
        int dummyAmmo = iGun.getDummyAmmoAmount(stack);
        if (dummyAmmo <= 0) return;

        int maxAmmo = TimelessAPI.getCommonGunIndex(iGun.getGunId(stack))
                .map(gunIndex -> gunIndex.getGunData().getAmmoAmount())
                .orElse(0);
        if (maxAmmo <= 0) return;

        int magazineCount = Math.round((float) dummyAmmo / maxAmmo);
        iGun.setDummyAmmoAmount(stack, magazineCount * maxAmmo);
    }

}

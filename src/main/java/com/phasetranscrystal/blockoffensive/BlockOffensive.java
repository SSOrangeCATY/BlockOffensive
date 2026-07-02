package com.phasetranscrystal.blockoffensive;

import com.phasetranscrystal.blockoffensive.command.CSCommand;
import com.phasetranscrystal.blockoffensive.client.BOClientBootstrap;
import com.phasetranscrystal.blockoffensive.compat.BOImpl;
import com.phasetranscrystal.blockoffensive.compat.BOMenuIntegration;
import com.phasetranscrystal.blockoffensive.compat.CSGrenadeCompat;
import com.phasetranscrystal.blockoffensive.compat.PhysicsModCompat;
import com.phasetranscrystal.blockoffensive.entity.BOEntityRegister;
import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.blockoffensive.map.team.capability.ColoredPlayerCapability;
import com.phasetranscrystal.blockoffensive.net.*;
import com.phasetranscrystal.blockoffensive.net.bomb.BombActionC2SPacket;
import com.phasetranscrystal.blockoffensive.net.bomb.BombActionS2CPacket;
import com.phasetranscrystal.blockoffensive.net.bomb.BombDemolitionProgressS2CPacket;
import com.phasetranscrystal.blockoffensive.net.dm.PlayerMoveC2SPacket;
import com.phasetranscrystal.blockoffensive.net.mvp.MvpHUDCloseS2CPacket;
import com.phasetranscrystal.blockoffensive.net.mvp.MvpMessageS2CPacket;
import com.phasetranscrystal.blockoffensive.net.shop.ShopStatesS2CPacket;
import com.phasetranscrystal.blockoffensive.net.spec.*;
import com.phasetranscrystal.blockoffensive.sound.BOSoundRegister;
import com.phasetranscrystal.blockoffensive.util.BOUtil;
import com.phasetranscrystal.blockoffensive.util.ThrowableType;
import com.phasetranscrystal.fpsmatch.common.item.FPSMItemRegister;
import com.phasetranscrystal.fpsmatch.common.packet.register.NetworkPacketRegister;
import com.phasetranscrystal.fpsmatch.common.sound.FPSMSoundRegister;
import com.phasetranscrystal.fpsmatch.compat.gun.GunTabTypeEnum;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(BlockOffensive.MODID)
public class BlockOffensive {
    public static final String MODID = "blockoffensive";
    private static final String PROTOCOL_VERSION = "1.3.0";
    private static final NetworkPacketRegister PACKET_REGISTER = new NetworkPacketRegister(Identifier.fromNamespaceAndPath(MODID, "main"), PROTOCOL_VERSION);

    public BlockOffensive(IEventBus modEventBus, ModContainer container)
    {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPackets);
        modEventBus.addListener(PACKET_REGISTER::registerPayloadHandlers);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modEventBus.addListener(BOClientBootstrap::onClientSetup);
            modEventBus.addListener(BOClientBootstrap::onRegisterEntityRenderEvent);
        }
        NeoForge.EVENT_BUS.register(this);
        BOItemRegister.ITEMS.register(modEventBus);
        BOItemRegister.TABS.register(modEventBus);
        BOEntityRegister.ENTITY_TYPES.register(modEventBus);
        BOSoundRegister.SOUNDS.register(modEventBus);
        container.registerConfig(ModConfig.Type.CLIENT, BOConfig.clientSpec);
        container.registerConfig(ModConfig.Type.COMMON, BOConfig.commonSpec);
        if (FMLEnvironment.getDist() == Dist.CLIENT && BOImpl.isClothConfigLoaded()) {
            BOMenuIntegration.registerModsPage(container);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CSCommand.onRegisterCommands(event);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ColoredPlayerCapability.register();

            FPSMSoundRegister.registerGunPickupSound(GunTabTypeEnum.PISTOL,BOSoundRegister.WEAPON_PISTOL_PICKUP.get());
            FPSMSoundRegister.registerGunPickupSound(GunTabTypeEnum.RIFLE,BOSoundRegister.WEAPON_RIFLE_PICKUP.get());
            FPSMSoundRegister.registerGunPickupSound(GunTabTypeEnum.SHOTGUN,BOSoundRegister.WEAPON_SHOTGUN_PICKUP.get());
            FPSMSoundRegister.registerGunPickupSound(GunTabTypeEnum.SMG,BOSoundRegister.WEAPON_SMG_PICKUP.get());
            FPSMSoundRegister.registerGunPickupSound(GunTabTypeEnum.SNIPER,BOSoundRegister.WEAPON_SNIPER_PICKUP.get());
            FPSMSoundRegister.registerGunPickupSound(GunTabTypeEnum.MG,BOSoundRegister.WEAPON_PICKUP.get());
            FPSMSoundRegister.registerGunPickupSound(GunTabTypeEnum.RPG,BOSoundRegister.WEAPON_PICKUP.get());

            FPSMSoundRegister.registerGunDropSound(GunTabTypeEnum.PISTOL,BOSoundRegister.WEAPON_PISTOL_IMPACT.get());
            FPSMSoundRegister.registerGunDropSound(GunTabTypeEnum.SNIPER,BOSoundRegister.WEAPON_SNIPER_IMPACT.get());
            FPSMSoundRegister.registerGunDropSound(GunTabTypeEnum.RIFLE,BOSoundRegister.WEAPON_RIFLE_IMPACT.get());
            FPSMSoundRegister.registerGunDropSound(GunTabTypeEnum.SMG,BOSoundRegister.WEAPON_SMG_IMPACT.get());
            FPSMSoundRegister.registerGunDropSound(GunTabTypeEnum.SHOTGUN,BOSoundRegister.WEAPON_SHOTGUN_IMPACT.get());
            FPSMSoundRegister.registerGunDropSound(GunTabTypeEnum.MG,BOSoundRegister.WEAPON_HEAVY_IMPACT.get());
            FPSMSoundRegister.registerGunDropSound(GunTabTypeEnum.RPG,BOSoundRegister.WEAPON_HEAVY_IMPACT.get());

            FPSMSoundRegister.registerKnifeDropSound(BOSoundRegister.WEAPON_KNIFE_IMPACT.get());
            FPSMSoundRegister.registerItemPickupSound(BOItemRegister.C4.get(), SoundEvents.EXPERIENCE_ORB_PICKUP);
            FPSMSoundRegister.registerItemDropSound(BOItemRegister.C4.get(), BOSoundRegister.WEAPON_C4_IMPACT.get());

            BOUtil.registerThrowable(ThrowableType.SMOKE, FPSMItemRegister.SMOKE_SHELL.get());
            BOUtil.registerThrowable(ThrowableType.GRENADE, FPSMItemRegister.GRENADE.get());
            BOUtil.registerThrowable(ThrowableType.INCENDIARY_GRENADE, FPSMItemRegister.T_INCENDIARY_GRENADE.get());
            BOUtil.registerThrowable(ThrowableType.INCENDIARY_GRENADE, FPSMItemRegister.CT_INCENDIARY_GRENADE.get());
            BOUtil.registerThrowable(ThrowableType.FLASH_BANG, FPSMItemRegister.FLASH_BOMB.get());

            registerCompat();
        });
    }

    private void onRegisterPackets(final FMLCommonSetupEvent event) {
        PACKET_REGISTER.registerPacket(BombActionC2SPacket.class);
        PACKET_REGISTER.registerPacket(BombActionS2CPacket.class);
        PACKET_REGISTER.registerPacket(BombDemolitionProgressS2CPacket.class);
        PACKET_REGISTER.registerPacket(MvpHUDCloseS2CPacket.class);
        PACKET_REGISTER.registerPacket(MvpMessageS2CPacket.class);
        PACKET_REGISTER.registerPacket(ShopStatesS2CPacket.class);
        PACKET_REGISTER.registerPacket(CSGameSettingsS2CPacket.class);
        PACKET_REGISTER.registerPacket(CSTabRemovalS2CPacket.class);
        PACKET_REGISTER.registerPacket(DeathMessageS2CPacket.class);
        PACKET_REGISTER.registerPacket(PxDeathCompatS2CPacket.class);
        PACKET_REGISTER.registerPacket(PxRagdollRemovalCompatS2CPacket.class);
        PACKET_REGISTER.registerPacket(CSGameWeaponDataS2CPacket.class);
        PACKET_REGISTER.registerPacket(BombFuseS2CPacket.class);
        PACKET_REGISTER.registerPacket(PlayerMoveC2SPacket.class);
        PACKET_REGISTER.registerPacket(KillCamS2CPacket.class);
        PACKET_REGISTER.registerPacket(RequestAttachTeammateC2SPacket.class);
        PACKET_REGISTER.registerPacket(RequestKillCamFallbackC2SPacket.class);
        PACKET_REGISTER.registerPacket(SwitchSpectateC2SPacket.class);
    }

    /**
     * 统一注册所有模组兼容层。
     * 由 {@code commonSetup} 在 enqueueWork 中调用，确保在主线程执行。
     */
    private static void registerCompat() {
        // 物理模组兼容
        if (BOImpl.isPhysicsModLoaded()) {
            if (FMLEnvironment.getDist() == Dist.CLIENT) {
                PhysicsModCompat.init();
            }
        }
        // CS Grenade 兼容
        if (ModList.get().isLoaded("csgrenades")) {
            CSGrenadeCompat.init();
        }
    }

    @SubscribeEvent
    public void onEnqueue(final InterModEnqueueEvent event) {
        event.enqueueWork(()->{
            if(BOImpl.isClothConfigLoaded()){
                // registered from the constructor with the NeoForge ModContainer
            }else{
                if (FMLEnvironment.getDist() == Dist.CLIENT) {
                    try {
                        Class<?> clothScreenClass = Class.forName("com.tacz.guns.client.gui.compat.ClothConfigScreen");
                        clothScreenClass.getMethod("registerNoClothConfigPage").invoke(null);
                    } catch (Exception ignored) {
                        // TACZ 未加载，无需注册
                    }
                }
            }
        });
    }

    public static <M> void sendTo(Player player, M message) {
        if (player.level().isClientSide()) {
            sendToServer(message);
        } else {
            sendToPlayer((ServerPlayer) player, message);
        }
    }

    public static <M> void sendToPlayer(ServerPlayer player, M message) {
        NetworkPacketRegister.getRegisterFromCache(message.getClass()).sendToPlayer(player, message);
    }

    public static <M> void sendToServer(M message) {
        NetworkPacketRegister.getRegisterFromCache(message.getClass()).sendToServer(message);
    }

    public static <M> void sendToAllPlayers(M message) {
        NetworkPacketRegister.getRegisterFromCache(message.getClass()).sendToAllPlayers(message);
    }
}

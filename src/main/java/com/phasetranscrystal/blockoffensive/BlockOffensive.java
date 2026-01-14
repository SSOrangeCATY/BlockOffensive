package com.phasetranscrystal.blockoffensive;

import com.phasetranscrystal.blockoffensive.command.CSCommand;
import com.phasetranscrystal.blockoffensive.compat.BOImpl;
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
import com.phasetranscrystal.blockoffensive.net.spec.BombFuseS2CPacket;
import com.phasetranscrystal.blockoffensive.net.spec.CSGameWeaponDataS2CPacket;
import com.phasetranscrystal.blockoffensive.sound.BOSoundRegister;
import com.phasetranscrystal.blockoffensive.util.BOUtil;
import com.phasetranscrystal.blockoffensive.util.ThrowableType;
import com.phasetranscrystal.fpsmatch.common.item.FPSMItemRegister;
import com.phasetranscrystal.fpsmatch.common.packet.register.NetworkPacketRegister;
import com.phasetranscrystal.fpsmatch.common.sound.FPSMSoundRegister;
import com.phasetranscrystal.fpsmatch.compat.impl.FPSMImpl;
import com.tacz.guns.api.item.GunTabType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(BlockOffensive.MODID)
public class BlockOffensive {
    public static final String MODID = "blockoffensive";
    private static final String PROTOCOL_VERSION = "1.3.0";
    private static final NetworkPacketRegister PACKET_REGISTER = new NetworkPacketRegister(ResourceLocation.tryBuild(MODID, "main"),PROTOCOL_VERSION);
    public static final SimpleChannel INSTANCE = PACKET_REGISTER.getChannel();

    @SuppressWarnings("removal")
    public BlockOffensive() {
        this(FMLJavaModLoadingContext.get());
    }

    public BlockOffensive(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        BOItemRegister.ITEMS.register(modEventBus);
        BOItemRegister.TABS.register(modEventBus);
        BOEntityRegister.ENTITY_TYPES.register(modEventBus);
        BOSoundRegister.SOUNDS.register(modEventBus);
        context.registerConfig(ModConfig.Type.CLIENT, BOConfig.clientSpec);
        context.registerConfig(ModConfig.Type.COMMON, BOConfig.commonSpec);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CSCommand.onRegisterCommands(event);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
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

        event.enqueueWork(() -> {
            ColoredPlayerCapability.register();

            FPSMSoundRegister.registerGunPickupSound(GunTabType.PISTOL,BOSoundRegister.WEAPON_PISTOL_PICKUP.get());
            FPSMSoundRegister.registerGunPickupSound(GunTabType.RIFLE,BOSoundRegister.WEAPON_RIFLE_PICKUP.get());
            FPSMSoundRegister.registerGunPickupSound(GunTabType.SHOTGUN,BOSoundRegister.WEAPON_SHOTGUN_PICKUP.get());
            FPSMSoundRegister.registerGunPickupSound(GunTabType.SMG,BOSoundRegister.WEAPON_SMG_PICKUP.get());
            FPSMSoundRegister.registerGunPickupSound(GunTabType.SNIPER,BOSoundRegister.WEAPON_SNIPER_PICKUP.get());
            FPSMSoundRegister.registerGunPickupSound(GunTabType.MG,BOSoundRegister.WEAPON_PICKUP.get());
            FPSMSoundRegister.registerGunPickupSound(GunTabType.RPG,BOSoundRegister.WEAPON_PICKUP.get());

            FPSMSoundRegister.registerGunDropSound(GunTabType.PISTOL,BOSoundRegister.WEAPON_PISTOL_IMPACT.get());
            FPSMSoundRegister.registerGunDropSound(GunTabType.SNIPER,BOSoundRegister.WEAPON_SNIPER_IMPACT.get());
            FPSMSoundRegister.registerGunDropSound(GunTabType.RIFLE,BOSoundRegister.WEAPON_RIFLE_IMPACT.get());
            FPSMSoundRegister.registerGunDropSound(GunTabType.SMG,BOSoundRegister.WEAPON_SMG_IMPACT.get());
            FPSMSoundRegister.registerGunDropSound(GunTabType.SHOTGUN,BOSoundRegister.WEAPON_SHOTGUN_IMPACT.get());
            FPSMSoundRegister.registerGunDropSound(GunTabType.MG,BOSoundRegister.WEAPON_HEAVY_IMPACT.get());
            FPSMSoundRegister.registerGunDropSound(GunTabType.RPG,BOSoundRegister.WEAPON_HEAVY_IMPACT.get());

            FPSMSoundRegister.registerKnifeDropSound(BOSoundRegister.WEAPON_KNIFE_IMPACT.get());
            FPSMSoundRegister.registerItemPickupSound(BOItemRegister.C4.get().asItem(), SoundEvents.EXPERIENCE_ORB_PICKUP);
            FPSMSoundRegister.registerItemDropSound(BOItemRegister.C4.get().asItem(), BOSoundRegister.WEAPON_C4_IMPACT.get());

            BOUtil.registerThrowable(ThrowableType.SMOKE, FPSMItemRegister.SMOKE_SHELL.get());
            BOUtil.registerThrowable(ThrowableType.GRENADE, FPSMItemRegister.GRENADE.get());
            BOUtil.registerThrowable(ThrowableType.INCENDIARY_GRENADE, FPSMItemRegister.T_INCENDIARY_GRENADE.get());
            BOUtil.registerThrowable(ThrowableType.INCENDIARY_GRENADE, FPSMItemRegister.CT_INCENDIARY_GRENADE.get());
            BOUtil.registerThrowable(ThrowableType.FLASH_BANG, FPSMItemRegister.FLASH_BOMB.get());

            if(BOImpl.isPhysicsModLoaded()){
                PhysicsModCompat.init();
            }

            if(FPSMImpl.findCounterStrikeGrenadesMod()){
                CSGrenadeCompat.init();
            }
        });
    }
}

package com.phasetranscrystal.blockoffensive;

import com.phasetranscrystal.blockoffensive.command.VoteCommand;
import com.phasetranscrystal.blockoffensive.entity.BOEntityRegister;
import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.blockoffensive.net.CSGameSettingsS2CPacket;
import com.phasetranscrystal.blockoffensive.net.CSTabRemovalS2CPacket;
import com.phasetranscrystal.blockoffensive.net.DeathMessageS2CPacket;
import com.phasetranscrystal.blockoffensive.net.bomb.BombActionC2SPacket;
import com.phasetranscrystal.blockoffensive.net.bomb.BombActionS2CPacket;
import com.phasetranscrystal.blockoffensive.net.bomb.BombDemolitionProgressS2CPacket;
import com.phasetranscrystal.blockoffensive.net.mvp.MvpHUDCloseS2CPacket;
import com.phasetranscrystal.blockoffensive.net.mvp.MvpMessageS2CPacket;
import com.phasetranscrystal.blockoffensive.net.shop.ShopStatesS2CPacket;
import com.phasetranscrystal.blockoffensive.sound.BOSoundRegister;
import com.phasetranscrystal.fpsmatch.common.packet.register.NetworkPacketRegister;
import com.rosymaple.hitindication.HitIndication;
import net.minecraft.resources.ResourceLocation;
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
    private static final String PROTOCOL_VERSION = "1.0.0";
    private static final NetworkPacketRegister PACKET_REGISTER = new NetworkPacketRegister(new ResourceLocation(MODID, "main"),PROTOCOL_VERSION);
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
        VoteCommand.onRegisterCommands(event);
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
    }
}

package com.phasetranscrystal.blockoffensive.event;

import com.phasetranscrystal.blockoffensive.BOConfig;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockOffensive.MODID,bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OBEvents {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player != null) {
            if(BOConfig.common.showLogin.get()){
                player.displayClientMessage(Component.translatable("blockoffensive.screen.scale.warm").withStyle(ChatFormatting.GRAY), false);
                player.displayClientMessage(Component.translatable("blockoffensive.screen.scale.warm.tips").withStyle(ChatFormatting.GRAY), false);
                player.displayClientMessage(Component.translatable("blockoffensive.login.message.closeable").withStyle(ChatFormatting.GRAY), false);
            }
        }
    }
}

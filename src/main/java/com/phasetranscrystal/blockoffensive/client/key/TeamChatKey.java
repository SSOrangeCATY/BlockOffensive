package com.phasetranscrystal.blockoffensive.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.phasetranscrystal.blockoffensive.client.screen.TeamChatScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT)
public class TeamChatKey {
    public static final KeyMapping TEAM_CHAT_KEY = new KeyMapping("key.blockoffensive.team_chat.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            OpenShopKey.CATEGORY);

    @SubscribeEvent
    public static void onTeamChatPress(ClientTickEvent.Post event) {        if(TEAM_CHAT_KEY.isDown()) {
            Minecraft.getInstance().gui.setScreen(new TeamChatScreen());
        }
    }
}

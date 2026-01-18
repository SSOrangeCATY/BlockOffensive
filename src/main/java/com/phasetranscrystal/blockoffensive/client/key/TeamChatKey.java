package com.phasetranscrystal.blockoffensive.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.phasetranscrystal.blockoffensive.client.screen.TeamChatScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TeamChatKey {
    public static final KeyMapping TEAM_CHAT_KEY = new KeyMapping("key.blockoffensive.team_chat.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "key.category.blockoffensive");

    @SubscribeEvent
    public static void onTeamChatPress(InputEvent.Key event) {
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS && TEAM_CHAT_KEY.matches(event.getKey(), event.getScanCode())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            
            Minecraft.getInstance().setScreen(new TeamChatScreen(""));
        }
    }
}
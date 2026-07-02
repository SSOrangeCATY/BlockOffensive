package com.phasetranscrystal.blockoffensive.client.screen;

import com.phasetranscrystal.blockoffensive.util.BOUtil;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.core.team.ClientTeam;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TeamChatScreen extends ChatScreen {
    public static final MutableComponent TITLE = Component.translatable("blockoffensive.team_chat.title");

    public TeamChatScreen() {
        super("", false);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.extractRenderState(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        if(this.input.getValue().isEmpty()){
            int x = 4;
            int y = this.height - 12;
            pGuiGraphics.text(minecraft.font,TITLE,x,y,BOUtil.getTeamColor(minecraft.player.getUUID()));
        }
    }
    
    @Override
    public void handleChatInput(String pInput, boolean pAddToRecentChat) {
        if (!pInput.isEmpty()) {
            if(pInput.startsWith("/")){
                super.handleChatInput(pInput, pAddToRecentChat);
                return;
            }else{
                if (pAddToRecentChat) {
                    this.minecraft.gui.hud.getChat().addRecentChat(pInput);
                }
            }
            MutableComponent teamMessage = BOUtil.buildTeamChatMessage(Component.literal(pInput));
            FPSMClient.getGlobalData().getCurrentClientTeam().ifPresent(team -> team.sendMessage(teamMessage));
        }
    }

    @Override
    public @NotNull Component getTitle() {
        return TITLE;
    }
}

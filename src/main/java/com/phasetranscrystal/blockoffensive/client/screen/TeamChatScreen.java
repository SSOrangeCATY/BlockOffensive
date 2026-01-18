package com.phasetranscrystal.blockoffensive.client.screen;

import com.phasetranscrystal.blockoffensive.util.BOUtil;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.core.team.ClientTeam;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TeamChatScreen extends ChatScreen {
    public static final MutableComponent TITLE = Component.translatable("blockoffensive.team_chat.title");

    public TeamChatScreen(String defaultText) {
        super(defaultText);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        if(this.input.getValue().isEmpty()){
            int x = 4;
            int y = this.height - 12;
            pGuiGraphics.drawString(minecraft.font,TITLE,x,y,BOUtil.getTeamColor(minecraft.player.getUUID()));
        }
    }
    
    @Override
    public boolean handleChatInput(String pInput, boolean pAddToRecentChat) {
        if (!pInput.isEmpty()) {
            if(pInput.startsWith("/")){
                super.handleChatInput(pInput, pAddToRecentChat);
                return minecraft.screen == this;
            }else{
                if (pAddToRecentChat) {
                    this.minecraft.gui.getChat().addRecentChat(pInput);
                }
            }
            MutableComponent teamMessage = BOUtil.buildTeamChatMessage(Component.literal(pInput));
            FPSMClient.getGlobalData().getCurrentClientTeam().ifPresent(team -> team.sendMessage(teamMessage));
        }
        return minecraft.screen == this;
    }

    @Override
    public @NotNull Component getTitle() {
        return TITLE;
    }
}
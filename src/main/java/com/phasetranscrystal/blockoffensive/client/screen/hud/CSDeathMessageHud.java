package com.phasetranscrystal.blockoffensive.client.screen.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.phasetranscrystal.blockoffensive.BOConfig;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.compat.BOImpl;
import com.phasetranscrystal.blockoffensive.compat.CSGrenadeCompat;
import com.phasetranscrystal.blockoffensive.data.DeathMessage;
import com.phasetranscrystal.blockoffensive.item.BOItemRegister;
import com.phasetranscrystal.fpsmatch.common.item.FPSMItemRegister;
import com.phasetranscrystal.fpsmatch.compat.CounterStrikeGrenadesCompat;
import com.phasetranscrystal.fpsmatch.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("all")
public class CSDeathMessageHud{
    private final Object queueLock = new Object();
    private final LinkedList<MessageData> messageQueue = new LinkedList<>();
    public final Minecraft minecraft;
    private final Map<String, ResourceLocation> specialKillIcons = new HashMap<>();
    private final Map<ResourceLocation, String> itemToIcon = new HashMap<>();

    public CSDeathMessageHud() {
        minecraft = Minecraft.getInstance();
        // 注册特殊击杀图标
        registerSpecialKillIcon("headshot", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/headshot.png"));
        registerSpecialKillIcon("throw_wall", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/throw_wall.png"));
        registerSpecialKillIcon("throw_smoke", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/throw_smoke.png"));
        registerSpecialKillIcon("explode", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/explode.png"));
        registerSpecialKillIcon("suicide", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/suicide.png"));
        registerSpecialKillIcon("fire", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/fire.png"));
        registerSpecialKillIcon("blindness", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/blindness.png"));
        registerSpecialKillIcon("no_zoom", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/no_zoom.png"));
        registerSpecialKillIcon("ct_incendiary_grenade", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/ct_incendiary_grenade.png"));
        registerSpecialKillIcon("grenade", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/grenade.png"));
        registerSpecialKillIcon("t_incendiary_grenade", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/t_incendiary_grenade.png"));
        registerSpecialKillIcon("flash_bomb", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/flash_bomb.png"));
        registerSpecialKillIcon("smoke_shell", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/smoke_shell.png"));
        registerSpecialKillIcon("fly", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/fly.png"));
        registerSpecialKillIcon("hand", ResourceLocation.tryBuild(BlockOffensive.MODID, "textures/ui/cs/message/hand.png"));

        registerSpecialKillIcon(ForgeRegistries.ITEMS.getKey(Items.AIR),"hand");
        registerSpecialKillIcon(ForgeRegistries.ITEMS.getKey(FPSMItemRegister.CT_INCENDIARY_GRENADE.get()),"ct_incendiary_grenade");
        registerSpecialKillIcon(ForgeRegistries.ITEMS.getKey(FPSMItemRegister.T_INCENDIARY_GRENADE.get()),"t_incendiary_grenade");
        registerSpecialKillIcon(ForgeRegistries.ITEMS.getKey(FPSMItemRegister.GRENADE.get()),"grenade");
        registerSpecialKillIcon(ForgeRegistries.ITEMS.getKey(FPSMItemRegister.FLASH_BOMB.get()),"flash_bomb");
        registerSpecialKillIcon(ForgeRegistries.ITEMS.getKey(FPSMItemRegister.SMOKE_SHELL.get()),"smoke_shell");
        registerSpecialKillIcon(ForgeRegistries.ITEMS.getKey(BOItemRegister.C4.get()),"explode");

        if(BOImpl.isCounterStrikeGrenadesLoaded()){
            CSGrenadeCompat.registerKillIcon(itemToIcon);
        }
    }

    public void render(GuiGraphics guiGraphics) {
        if (BOConfig.client.killMessageHudEnabled.get() && !messageQueue.isEmpty()) {
            if (minecraft.player != null) {
                renderKillTips(guiGraphics);
            }
        }
    }

    private void renderKillTips(GuiGraphics guiGraphics) {
        long currentTime = System.currentTimeMillis();
        int yOffset = getHudPositionYOffset();

        synchronized(queueLock) {
            messageQueue.removeIf(messageData ->
                    currentTime - messageData.displayStartTime >= BOConfig.client.messageShowTime.get() * 1000);

            for (MessageData messageData : messageQueue) {
                DeathMessage message = messageData.message;

                int width = calculateMessageWidth(message);
                int x = getHudPositionXOffset(width);

                renderKillMessage(guiGraphics, message, x, yOffset);

                yOffset += 16;
            }
        }
    }

    public void addKillMessage(DeathMessage message) {
        synchronized(queueLock) {
            long currentTime = System.currentTimeMillis();

            messageQueue.removeIf(messageData ->
                    currentTime - messageData.displayStartTime >= BOConfig.client.messageShowTime.get() * 1000);

            if (messageQueue.size() >= BOConfig.client.maxShowCount.get()) {
                messageQueue.removeFirst();
            }

            messageQueue.add(new MessageData(message, currentTime));
        }
    }

    private int getHudPositionYOffset() {
        return switch (BOConfig.client.killMessageHudPosition.get()) {
            case 1, 2 -> 10;
            default -> minecraft.getWindow().getGuiScaledHeight() - 10 * 5;
        };
    }

    private int getHudPositionXOffset(int stringWidth) {
        return switch (BOConfig.client.killMessageHudPosition.get()) {
            case 2, 4 -> minecraft.getWindow().getGuiScaledWidth() - 10 - stringWidth;
            default -> 10;
        };
    }

    public void registerSpecialKillIcon(String id, ResourceLocation texture) {
        specialKillIcons.put(id, texture);
    }

    public void registerSpecialKillIcon(ResourceLocation item, String id) {
        itemToIcon.put(item, id);
    }

    private void renderKillMessage(GuiGraphics guiGraphics, DeathMessage message, int x, int y) {
        PoseStack poseStack = guiGraphics.pose();
        Font font = minecraft.font;
        UUID local = minecraft.player.getUUID();
        boolean isLocalPlayer = message.getKillerUUID().equals(local) || message.getAssistUUID().equals(local);

        int width = calculateMessageWidth(message);
        int height = 16;
        int bgColor = 0x80000000;

        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        if (isLocalPlayer) {
            guiGraphics.fill(x, y, x + width, y + 1, 0xFFFF0000);
            guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFFFF0000);
            guiGraphics.fill(x, y, x + 1, y + height, 0xFFFF0000);
            guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFFFF0000);
        }

        boolean isSuicide = message.getDeadUUID().equals(message.getKillerUUID()) || message.getWeapon().getItem() == BOItemRegister.C4.get();

        int currentX = x + 5;
        int rightPadding = x + width - 5;

        if (message.isBlinded()) {
            renderIcon(guiGraphics, specialKillIcons.get("blindness"), currentX, y + 2, 12, 12);
            currentX += 14;
        }

        MutableComponent component = isSuicide ? message.getDead().copy() : message.getKiller().copy();
        if(!message.getAssistUUID().equals(message.getKillerUUID())){
            component.append(" + ");
            component.append(message.getAssist());
        };

        guiGraphics.drawString(font, component, currentX, y + 4, -1, true);
        currentX += font.width(component) + 2;

        if (message.isFlying()) currentX = renderConditionalIcon(guiGraphics, "fly", currentX, y);

        if(!isSuicide){
            ResourceLocation weaponIcon = message.getWeaponIcon();
            poseStack.pushPose();
            poseStack.translate(currentX, y + 1, 0);
            if (weaponIcon != null) {
                poseStack.scale(0.32f, 0.32f, 1.0f);
                renderWeaponIcon(guiGraphics, weaponIcon);
                currentX += 39;
            }else{
                if(!this.itemToIcon.containsKey(message.getItemRL())){
                    guiGraphics.renderItem(message.getWeapon(),0,0);
                    currentX += 16;
                }
            }
            poseStack.popPose();

            String icon = this.itemToIcon.getOrDefault(message.getItemRL(),null);
            if(icon != null){
                weaponIcon = this.specialKillIcons.getOrDefault(icon,null);
                if(weaponIcon != null) {
                    renderIcon(guiGraphics, weaponIcon, currentX, y + 2, 12, 12);
                    currentX += 14;
                }
            }
        }else{
            renderIcon(guiGraphics, this.specialKillIcons.get("suicide"), currentX, y + 2, 12, 12);
            currentX += 14;
        }

        if (message.isHeadShot()) currentX = renderConditionalIcon(guiGraphics, "headshot", currentX, y);
        if (message.isThroughSmoke()) currentX = renderConditionalIcon(guiGraphics, "throw_smoke", currentX, y);
        if (message.isThroughWall()) currentX = renderConditionalIcon(guiGraphics, "throw_wall", currentX, y);
        if (message.isNoScope()) currentX = renderConditionalIcon(guiGraphics, "no_zoom", currentX, y);

        int deadNameWidth = font.width(message.getDead());
        currentX = Math.min(currentX, rightPadding - deadNameWidth);
        guiGraphics.drawString(font, message.getDead(), currentX, y + 4, -1, true);
    }

    private int renderConditionalIcon(GuiGraphics guiGraphics, String iconKey, int currentX, int y) {
        renderIcon(guiGraphics, specialKillIcons.get(iconKey), currentX, y + 2, 12, 12);
        return currentX + 14;
    }

    private void renderIcon(GuiGraphics guiGraphics, ResourceLocation icon, int x, int y, int width, int height) {
        guiGraphics.blit(icon, x, y, 0, 0, width, height, width, height);
    }

    private void renderWeaponIcon(GuiGraphics guiGraphics, ResourceLocation icon) {
        RenderUtil.renderReverseTexture(guiGraphics,icon, 0, 0, 117, 44);
    }

    private int calculateMessageWidth(DeathMessage message) {
        Font font = minecraft.font;
        int width = 10;

        boolean isSuicide = message.getDeadUUID().equals(message.getKillerUUID()) || message.getWeapon().getItem() == BOItemRegister.C4.get();

        if (message.isBlinded()) {
            width += 14;
        }

        MutableComponent killerComponent = isSuicide ? message.getDead().copy() : message.getKiller().copy();
        if (!message.getAssistUUID().equals(message.getKillerUUID())) {
            killerComponent.append(" + ").append(message.getAssist());
        }
        width += font.width(killerComponent) + 2;

        if(!isSuicide){
            ResourceLocation weaponIcon = message.getWeaponIcon();
            if (weaponIcon != null) {
                width += 39;
            } else {
                if (!this.itemToIcon.containsKey(message.getItemRL())) {
                    width += 16;
                }
            }

            String specialIcon = this.itemToIcon.getOrDefault(message.getItemRL(), null);
            if (specialIcon != null && this.specialKillIcons.containsKey(specialIcon)) {
                width += 14;
            }
        }else{
            width += 14;
        }

        if (message.isFlying()) width += 14;
        if (message.isHeadShot()) width += 14;
        if (message.isThroughSmoke()) width += 14;
        if (message.isThroughWall()) width += 14;
        if (message.isNoScope()) width += 14;

        width += font.width(message.getDead());

        return width;
    }

    public void reset(){
        messageQueue.clear();
    }

    public record MessageData(DeathMessage message, long displayStartTime) {
    }
}
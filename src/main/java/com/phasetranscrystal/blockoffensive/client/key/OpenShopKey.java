package com.phasetranscrystal.blockoffensive.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import icyllis.modernui.mc.MuiModApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

import static com.phasetranscrystal.fpsmatch.compat.gun.GunCompatManager.isInGame;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT)
public class OpenShopKey {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.withDefaultNamespace("blockoffensive"));
    public static final KeyMapping.Category SPEC_CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("blockoffensive", "spec"));

    public static final KeyMapping OPEN_SHOP_KEY = new KeyMapping("key.blockoffensive.open.shop.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY);

    public static int lastGuiScaleOption = -1;

    public static int getLastGuiScaleOption(){
        return lastGuiScaleOption;
    }

    public static void resetLastGuiScaleOption(){
        lastGuiScaleOption = -1;
    }

    @SubscribeEvent
    public static void onInspectPress(InputEvent.Key event) {
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS && OPEN_SHOP_KEY.matches(new KeyEvent(event.getKey(), event.getScanCode(), event.getModifiers()))) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }

            if(CSClientData.isDebug){
                openShop();
            }else{
                if(FPSMClient.getGlobalData().isCurrentMap("fpsm_none")){
                    Minecraft.getInstance().player.sendSystemMessage(Component.translatable("key.blockoffensive.open.shop.failed.no_map"));
                    return;
                }

                if(!CSClientData.canOpenShop){
                    Minecraft.getInstance().player.sendSystemMessage(Component.translatable("key.blockoffensive.open.shop.failed.purchase_time.expired"));
                    return;
                }

                if(CSClientData.isStart){
                    openShop();
                }else{
                    Minecraft.getInstance().player.sendSystemMessage(Component.translatable("key.blockoffensive.open.shop.failed.game.not_started"));
                }
            }
        }
    }

    private static void openShop(){
        Minecraft minecraft = Minecraft.getInstance();
        int guiScaleOption = minecraft.options.guiScale().get();
        if(guiScaleOption != 2) {
            lastGuiScaleOption = guiScaleOption;
            minecraft.options.guiScale().set(2);
            minecraft.resizeGui();
        }
        MuiModApi.openScreen(CSGameShopScreen.getInstance());
    }
}

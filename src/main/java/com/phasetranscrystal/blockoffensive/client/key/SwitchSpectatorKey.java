package com.phasetranscrystal.blockoffensive.client.key;

import com.phasetranscrystal.blockoffensive.net.spec.SwitchSpectateC2SPacket;
import com.phasetranscrystal.blockoffensive.spectator.BOSpecManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;


@EventBusSubscriber(value = Dist.CLIENT)
public class SwitchSpectatorKey {
    public static final KeyMapping KEY_SPECTATE_PREV = new KeyMapping(
            "key.blockoffensive.switch_spec_previous.desc", GLFW.GLFW_KEY_A, OpenShopKey.SPEC_CATEGORY);
    public static final KeyMapping KEY_SPECTATE_NEXT = new KeyMapping(
            "key.blockoffensive.switch_spec_next.desc", GLFW.GLFW_KEY_D, OpenShopKey.SPEC_CATEGORY);

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            if (KEY_SPECTATE_PREV.consumeClick()) {
                BOSpecManager.sendSwitchSpectate(SwitchSpectateC2SPacket.SwitchDirection.PREV);
            } else if (KEY_SPECTATE_NEXT.consumeClick()) {
                BOSpecManager.sendSwitchSpectate(SwitchSpectateC2SPacket.SwitchDirection.NEXT);
            }
        }
    }
}

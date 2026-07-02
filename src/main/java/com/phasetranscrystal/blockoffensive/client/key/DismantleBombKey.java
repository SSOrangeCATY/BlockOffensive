package com.phasetranscrystal.blockoffensive.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.net.bomb.BombActionC2SPacket;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.phasetranscrystal.fpsmatch.compat.gun.GunCompatManager;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT)
public class DismantleBombKey {
    public static final KeyMapping DISMANTLE_BOMB_KEY = new KeyMapping("key.blockoffensive.dismantle_bomb.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_E,
            OpenShopKey.CATEGORY);

    @SubscribeEvent
    public static void onInspectPress(InputEvent.Key event) {
        boolean inGame = GunCompatManager.isInGame();
        if(inGame && DISMANTLE_BOMB_KEY.isDown()){
            if (event.getAction() == GLFW.GLFW_PRESS) {
                BlockOffensive.sendToServer(new BombActionC2SPacket(true));
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                BlockOffensive.sendToServer(new BombActionC2SPacket(false));
            }
        }
    }

}

package com.xuebi1145.xuplus_client.mixin.c4;

import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameHud;
import com.xuebi1145.xuplus_client.hud.c4.C4TimerHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CSGameHud.class, remap = false)
public class MixinCSGameHudC4Timer {
    @Unique
    private final C4TimerHUD xuplus$c4TimerHud = new C4TimerHUD();

    @Inject(method = "renderInfoLine", at = @At("TAIL"), remap = false)
    private void xuplus$renderC4Timer(Minecraft mc, ForgeGui gui, GuiGraphics guiGraphics, int screenWidth, int screenHeight, CallbackInfo ci) {
        xuplus$c4TimerHud.render(guiGraphics, screenWidth, screenHeight);
    }
}

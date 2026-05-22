package com.xuebi1145.xuplus_client.mixin;

import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CSGameOverlay.class, remap = false)
public class MixinCSGameOverlayMoney {

    private static final ResourceLocation XUPLUS_SHOP_ICON = new ResourceLocation("xuplus_client", "textures/cs2/shop.png");

    @Inject(method = "renderMoneyText", at = @At("TAIL"))
    private void xuplus$appendShopIcon(GuiGraphics guiGraphics, int screenHeight, CallbackInfo ci) {
        if (CSClientData.shopCloseTime <= 0) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        String moneyText = "$ " + CSClientData.getMoney();
        int moneyWidth = font.width(moneyText);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(5, screenHeight - 20, 0);
        guiGraphics.pose().scale(2, 2, 0);
        guiGraphics.blit(XUPLUS_SHOP_ICON, moneyWidth + 5, -1, 0, 0, 8, 8, 8, 8);
        guiGraphics.pose().popPose();
    }
}

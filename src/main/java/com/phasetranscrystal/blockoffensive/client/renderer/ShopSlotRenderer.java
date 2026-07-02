package com.phasetranscrystal.blockoffensive.client.renderer;

import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.map.shop.ItemType;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.shop.ClientShopSlot;
import com.phasetranscrystal.fpsmatch.compat.gun.GunCompatManager;
import com.phasetranscrystal.fpsmatch.util.FPSMUtil;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.GunDisplayInstance;
import icyllis.modernui.mc.MinecraftSurfaceView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;


public class ShopSlotRenderer implements MinecraftSurfaceView.Renderer {
    public final ItemType type;
    public final int index;
    public float scale = 1;

    public ShopSlotRenderer(ItemType type, int index) {
        this.type = type;
        this.index = index;
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
    }

    @Override
    public void onDraw(@NotNull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTick, double guiScale, float alpha) {
        ClientShopSlot currentSlot = FPSMClient.getGlobalData().getSlotData(this.type.name(), this.index);
        ItemStack itemStack = currentSlot.itemStack();
        boolean enable = CSClientData.getMoney() >= currentSlot.cost() && !itemStack.isEmpty() && !currentSlot.isLocked();
        gr.pose().pushMatrix();
        Optional<GunDisplayInstance> display = ModList.get().isLoaded("tacz") ? TimelessAPI.getGunDisplay(itemStack) : Optional.empty();
        // gr.fill(0,0,1920,1080, RenderUtil.color(124,66,232));
        if(display.isPresent()){
            float offset = 0;
            if (GunCompatManager.isGun(itemStack)){
                Optional<com.phasetranscrystal.fpsmatch.compat.gun.GunTabTypeEnum> type = FPSMUtil.getGunTypeByGunId(GunCompatManager.findProvider(itemStack).getGunId(itemStack));
                if(type.isPresent() && type.get() == com.phasetranscrystal.fpsmatch.compat.gun.GunTabTypeEnum.PISTOL){
                    offset = 10f;
                }
            }
            this.renderIcon(gr,display.get(),enable,offset);
        }else{
            this.renderItem(gr,itemStack,enable);
        }
        gr.pose().popMatrix();
    }

    public void renderItem(GuiGraphicsExtractor gr,ItemStack itemStack,boolean enable){
        this.setItemColor(gr,enable);
        gr.pose().scale(scale,scale);
        gr.item(itemStack, 2, 0);
    }

    public void renderIcon(GuiGraphicsExtractor gr,GunDisplayInstance display,boolean enable,float offset){
        int color = iconColor(enable);
        gr.blit(RenderPipelines.GUI_TEXTURED, display.getHUDTexture(),0, (int) (8*scale), offset*scale, 0.0F, (int) (58.5F*scale), (int) (19.5F*scale), (int) (58.5F*scale), (int) (19.5F*scale), color);
    }

    public void setIconColor(GuiGraphicsExtractor gr,boolean enable){
    }

    private int iconColor(boolean enable) {
        if(enable){
            if(FPSMClient.getGlobalData().isCurrentTeam("ct")){
                return 0xFF96C8FA;
            }else{
                return 0xFFEAC055;
            }
        }else{
            return 0xFF7D7D7D;
        }
    }

    public void setItemColor(GuiGraphicsExtractor gr,boolean enable){
    }

    public void setScale(float scale) {
        this.scale = scale;
    }
}

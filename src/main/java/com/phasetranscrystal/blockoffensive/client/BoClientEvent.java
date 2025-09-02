package com.phasetranscrystal.blockoffensive.client;

import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.key.OpenShopKey;
import com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen;
import com.phasetranscrystal.blockoffensive.compat.BOImpl;
import com.phasetranscrystal.blockoffensive.compat.CounterStrikeGrenadesCompat;
import com.phasetranscrystal.blockoffensive.compat.LrtacticalCompat;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.event.FPSMClientResetEvent;
import com.phasetranscrystal.fpsmatch.core.item.IThrowEntityAble;
import com.phasetranscrystal.fpsmatch.impl.FPSMImpl;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.InputExtraCheck;
import icyllis.modernui.mc.MuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockOffensive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BoClientEvent {
    @SubscribeEvent
    public static void onClientTickEvent(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        lockMove(mc);

        pullMessage();

        checkOption(mc);
    }

    public static void checkOption(Minecraft mc){
        if(OpenShopKey.getLastGuiScaleOption() == -1) return;
        boolean isShop = mc.screen instanceof MuiScreen muiScreen && muiScreen.getFragment() instanceof CSGameShopScreen;
        if(!isShop && OpenShopKey.getLastGuiScaleOption() != mc.options.guiScale().get()){
            mc.options.guiScale().set(OpenShopKey.getLastGuiScaleOption());
            mc.resizeDisplay();
            OpenShopKey.resetLastGuiScaleOption();
        }
    }

    public static void pullMessage(){
        if(CSClientData.isStart && (FPSMClient.getGlobalData().equalsMap("fpsm_none") || FPSMClient.getGlobalData().equalsGame("none"))){
            FPSMatch.pullGameInfo();
        }
    }
    
    public static void lockMove(Minecraft mc){
        LocalPlayer player = mc.player;
        if(player == null) return;
        if((CSClientData.isWaiting || CSClientData.isPause) && (FPSMClient.getGlobalData().equalsGame("cs") && !FPSMClient.getGlobalData().isSpectator())){
            mc.options.keyUp.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyJump.setDown(false);
        }
    }

    @SubscribeEvent
    public static void onFPSMClientReset(FPSMClientResetEvent event) {
        CSClientData.reset();
    }


    @SubscribeEvent
    public static void onUse(InputEvent.MouseButton.Pre event){
        if((CSClientData.isWaiting || CSClientData.isPause) && InputExtraCheck.isInGame()){
            if(checkLocalPlayerHand()){
                event.setCanceled(true);
            }
        }
    }

    public static boolean checkLocalPlayerHand(){
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            Item main = player.getMainHandItem().getItem();
            Item off = player.getOffhandItem().getItem();
            return itemCheck(main) || itemCheck(off)
                    || (FPSMImpl.findEquipmentMod() && (LrtacticalCompat.itemCheck(main) || LrtacticalCompat.itemCheck(off))
                    || (BOImpl.isCounterStrikeGrenadesLoaded() && (CounterStrikeGrenadesCompat.itemCheck(main) || CounterStrikeGrenadesCompat.itemCheck(off))));
        }
        return false;
    }

    private static boolean itemCheck(Item item){
        return item instanceof IGun || item instanceof IThrowEntityAble;
    }


}

package com.phasetranscrystal.blockoffensive.client;

import com.phasetranscrystal.blockoffensive.BOConfig;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.key.OpenShopKey;
import com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen;
import com.phasetranscrystal.blockoffensive.web.BOClientWebServer;
import com.phasetranscrystal.blockoffensive.compat.BOImpl;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.data.FPSMClientGlobalData;
import com.phasetranscrystal.fpsmatch.common.client.event.FPSMClientResetEvent;
import com.phasetranscrystal.fpsmatch.compat.CounterStrikeGrenadesCompat;
import com.phasetranscrystal.fpsmatch.compat.LrtacticalCompat;
import com.phasetranscrystal.fpsmatch.compat.impl.FPSMImpl;
import com.phasetranscrystal.fpsmatch.core.item.IThrowEntityAble;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.InputExtraCheck;
import icyllis.modernui.mc.MuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockOffensive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BOClientEvent {
    @SubscribeEvent
    public static void onClientTickEvent(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        lockMove(mc);

        pullMessage();

        checkOption(mc);

        startWebServer();
    }

    public static void startWebServer(){
        if(!BOConfig.common.webServerEnabled.get()) return;
        FPSMClientGlobalData data = FPSMClient.getGlobalData();

        if(!(!data.isInMap() || !data.isInGame()) && data.isSpectator()) {
            BOClientWebServer.start();
        }else {
            BOClientWebServer.stop();
        }
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
        FPSMClientGlobalData data = FPSMClient.getGlobalData();
        if(CSClientData.isStart && (!data.isInMap() || !data.isInGame())){
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
            return itemCheck(player) || (FPSMImpl.findEquipmentMod() && LrtacticalCompat.itemCheck(player) || (BOImpl.isCounterStrikeGrenadesLoaded() && CounterStrikeGrenadesCompat.itemCheck(player)));
        }
        return false;
    }

    private static boolean itemCheck(Player player){
        Item main = player.getMainHandItem().getItem();
        Item off = player.getOffhandItem().getItem();
        return (main instanceof IGun || main instanceof IThrowEntityAble) || (off instanceof IGun || off instanceof IThrowEntityAble);
    }

}

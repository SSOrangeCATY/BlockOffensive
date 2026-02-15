package com.phasetranscrystal.blockoffensive.client;

import com.phasetranscrystal.blockoffensive.BOConfig;
import com.phasetranscrystal.blockoffensive.BlockOffensive;
import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.key.OpenShopKey;
import com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen;
import com.phasetranscrystal.blockoffensive.net.dm.PlayerMoveC2SPacket;
import com.phasetranscrystal.blockoffensive.util.BOUtil;
import com.phasetranscrystal.blockoffensive.util.ThrowableType;
import com.phasetranscrystal.blockoffensive.web.BOClientWebServer;
import com.phasetranscrystal.blockoffensive.compat.BOImpl;
import com.phasetranscrystal.fpsmatch.FPSMatch;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.client.data.FPSMClientGlobalData;
import com.phasetranscrystal.fpsmatch.common.client.event.FPSMClientResetEvent;
import com.phasetranscrystal.fpsmatch.common.drop.ThrowableRegistry;
import com.phasetranscrystal.fpsmatch.common.event.FPSMThrowGrenadeEvent;
import com.phasetranscrystal.fpsmatch.common.event.RequestSpectatorOutlinesEvent;
import com.phasetranscrystal.fpsmatch.common.packet.FPSMSoundPlayC2SPacket;
import com.phasetranscrystal.fpsmatch.compat.CounterStrikeGrenadesCompat;
import com.phasetranscrystal.fpsmatch.compat.LrtacticalCompat;
import com.phasetranscrystal.fpsmatch.compat.impl.FPSMImpl;
import com.phasetranscrystal.fpsmatch.core.item.IThrowEntityAble;
import com.phasetranscrystal.fpsmatch.core.team.ClientTeam;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.InputExtraCheck;
import icyllis.modernui.mc.MuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockOffensive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BOClientEvent {
    @SubscribeEvent
    public static void onClientTickEvent(TickEvent.ClientTickEvent event) {
        FPSMClientGlobalData data = FPSMClient.getGlobalData();
        if(CSClientData.isStart && (!data.isInMap() || !data.isInGame())){
            FPSMatch.pullGameInfo();
        }

        Minecraft mc = Minecraft.getInstance();
        lockMove(mc);

        checkOption(mc);

        if(BOConfig.common.webServerEnabled.get()){
            if(!(!data.isInMap() || !data.isInGame()) && data.isSpectator()) {
                BOClientWebServer.start();
            }else {
                BOClientWebServer.stop();
            }
        }
    }

    @SubscribeEvent
    public static void onRequestSpectatorOutlinesEvent(RequestSpectatorOutlinesEvent event){
        event.setCanceled(FPSMClient.getGlobalData().getCurrentClientTeam().map(ClientTeam::isNormal).orElse(false));
    }

    @SubscribeEvent
    public static void onPlayerMoveInput(MovementInputUpdateEvent event) {
        if(Minecraft.getInstance().player == null) return;
        Input input = event.getInput();
        if(!FPSMClient.getGlobalData().isCurrentGameType("csdm")) return;

        if (input.left || input.right || input.up || input.down || input.shiftKeyDown) {
            FPSMatch.sendToServer(new PlayerMoveC2SPacket());
        }
    }

    @SubscribeEvent
    public static void onFPSMatchThrowGrenade(FPSMThrowGrenadeEvent event){
        BOUtil.buildGrenadeMessageAndSend(event.getItemStack());
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
    
    public static void lockMove(Minecraft mc){
        LocalPlayer player = mc.player;
        if(player == null) return;
        if(isLocked()){
            mc.options.keyUp.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyJump.setDown(false);
        }
    }

    public static boolean isLocked(){
        return (CSClientData.isWaiting || CSClientData.isPause) && (FPSMClient.getGlobalData().isCurrentGameType("cs") && !FPSMClient.getGlobalData().isSpectator());
    }

    @SubscribeEvent
    public static void onFPSMClientReset(FPSMClientResetEvent event) {
        CSClientData.reset();
    }


    @SubscribeEvent
    public static void onInput(InputEvent.MouseButton.Pre event){
        if(isLocked() && InputExtraCheck.isInGame()){
            if(checkLocalPlayerHand()){
                if(event.getAction() == 1){
                    event.setCanceled(true);
                }
            }
        }
    }

    public static boolean checkLocalPlayerHand(){
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            return itemCheck(player) || (FPSMImpl.findLrtacticalMod() && LrtacticalCompat.itemCheck(player) || (BOImpl.isCounterStrikeGrenadesLoaded() && CounterStrikeGrenadesCompat.itemCheck(player)));
        }
        return false;
    }

    private static boolean itemCheck(Player player){
        Item main = player.getMainHandItem().getItem();
        Item off = player.getOffhandItem().getItem();
        return (main instanceof IGun || main instanceof IThrowEntityAble) || (off instanceof IGun || off instanceof IThrowEntityAble);
    }

}

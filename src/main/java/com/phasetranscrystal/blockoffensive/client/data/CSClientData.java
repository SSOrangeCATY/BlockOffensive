package com.phasetranscrystal.blockoffensive.client.data;

import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameHud;
import com.phasetranscrystal.blockoffensive.client.screen.hud.CSMvpHud;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import net.minecraft.client.Minecraft;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class CSClientData {
    public static boolean currentMapSupportShop = true;
    public static int cTWinnerRounds = 0;
    public static int tWinnerRounds = 0;
    public static int time = 0;
    public static boolean isDebug = false;
    public static boolean isStart = false;
    public static boolean isError = false;
    public static boolean isPause = false;
    public static boolean isWaiting = false;
    public static boolean isWarmTime = false;
    public static boolean isWaitingWinner = false;
    public static boolean canOpenShop = false;
    public static int shopCloseTime = 0;
    public static int nextRoundMoney = 0;
    public static float dismantleBombProgress = 0;

    public static boolean bpAttributeHasHelmet = false;
    public static int bpAttributeDurability = 0;

    public static int getMoney(){
        return FPSMClient.getGlobalData().getPlayerMoney(Minecraft.getInstance().player.getUUID());
    }

    public static void reset() {
        currentMapSupportShop = true;
        CSGameHud.getInstance().reset();
        cTWinnerRounds = 0;
        tWinnerRounds = 0;
        time = 0;
        isDebug = false;
        isStart = false;
        isError = false;
        isPause = false;
        isWaiting = false;
        isWarmTime = false;
        isWaitingWinner = false;
        nextRoundMoney = 0;
        canOpenShop = false;
        dismantleBombProgress = 0;
    }

    public static int getNextRoundMinMoney() {
        return nextRoundMoney;
    }

    public static PlayerData getLocalCSTabData(){
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            UUID uuid = mc.player.getUUID();
            Optional<PlayerData> data = FPSMClient.getGlobalData().getPlayerTabData(uuid);
            if(data.isPresent()){
                return data.get();
            }
        }
        return null;
    }

    public static int getLivingWithTeam(String team){
        AtomicReference<Integer> living = new AtomicReference<>(0);
        FPSMClient.getGlobalData().tabData.values().forEach((pair)->{
            if(pair.getFirst().equals(team) && pair.getSecond().isLivingNoOnlineCheck()){
                living.getAndSet(living.get() + 1);
            }
        });
        return living.get();
    }

}

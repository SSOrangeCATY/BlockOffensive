package com.phasetranscrystal.blockoffensive.client.data;

import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameHud;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.entity.drop.DropType;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import net.minecraft.client.Minecraft;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    // 旁观者数据
    public static int bombFuse = 0;
    public static int bombTotalFuse;

    public static final Map<UUID, WeaponData> weaponData = new ConcurrentHashMap<>();


    public static int getMoney() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;

        return FPSMClient.getGlobalData().getPlayerMoney(mc.player.getUUID());
    }

    public static WeaponData getWeaponData(UUID uuid) {
        return weaponData.getOrDefault(uuid, WeaponData.EMPTY);
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
        weaponData.clear();
    }

    public static int getNextRoundMinMoney() {
        return nextRoundMoney;
    }

    public static Optional<PlayerData> getLocalCSTabData() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            UUID uuid = mc.player.getUUID();
            return FPSMClient.getGlobalData().getPlayerTabData(uuid);
        }
        return Optional.empty();
    }

    public static int getLivingWithTeam(String team) {
        int living = 0;
        for (var clientTeam : FPSMClient.getGlobalData().clientTeamData.values()) {
            if (clientTeam.name.equals(team)) {
                for (var data :clientTeam.players.values()){
                    if (data.isLivingNoOnlineCheck()) living++;
                }
            }
        }
        return living;
    }
}

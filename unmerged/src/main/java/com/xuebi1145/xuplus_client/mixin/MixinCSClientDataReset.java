package com.xuebi1145.xuplus_client.mixin;

import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.xuebi1145.xuplus_client.hud.ClientBombItemCache;
import com.xuebi1145.xuplus_client.hud.ClientBombTimerCache;
import com.xuebi1145.xuplus_client.hud.ClientGrenadeCache;
import com.xuebi1145.xuplus_client.hud.ClientRoundKillCache;
import com.xuebi1145.xuplus_client.hud.ClientWeaponItemCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在CSClientData.reset()时同步清除XUPlus客户端缓存
 */
@Mixin(value = CSClientData.class, remap = false)
public class MixinCSClientDataReset {

    @Inject(method = "reset", at = @At("TAIL"), remap = false)
    private static void xuplus$clearCaches(CallbackInfo ci) {
        CSClientData.bombFuse = 0;
        CSClientData.bombTotalFuse = 0;
        ClientBombItemCache.clear();
        ClientBombTimerCache.clear();
        ClientGrenadeCache.clear();
        ClientWeaponItemCache.clear();
        ClientRoundKillCache.clear();
    }
}

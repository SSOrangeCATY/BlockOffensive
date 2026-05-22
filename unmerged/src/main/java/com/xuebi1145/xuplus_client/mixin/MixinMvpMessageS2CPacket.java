package com.xuebi1145.xuplus_client.mixin;

import com.phasetranscrystal.blockoffensive.data.MvpReason;
import com.phasetranscrystal.blockoffensive.net.mvp.MvpMessageS2CPacket;
import com.xuebi1145.xuplus_client.MusicBoxManager;
import com.xuebi1145.xuplus_client.PlayerVipManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraftforge.network.NetworkEvent;

/**
 * Mixin到MvpMessageS2CPacket，拦截MVP事件触发音乐盒
 */
@Mixin(MvpMessageS2CPacket.class)
public class MixinMvpMessageS2CPacket {
    
    @Shadow(remap = false) @Final
    private MvpReason mvpReason;
    
    @Inject(method = "handle", at = @At("HEAD"), remap = false)
    private void onMvpHandle(Supplier<NetworkEvent.Context> ctx, CallbackInfo ci) {
        UUID mvpUuid = mvpReason.uuid;
        // 触发音乐盒
        MusicBoxManager.onMvpTriggered(mvpUuid);
    }
}

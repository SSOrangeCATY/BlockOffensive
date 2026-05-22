package com.xuebi1145.xuplus_client.mixin.c4;

import com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CompositionC4Entity.class, remap = false)
public class MixinC4Persistence {

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"), remap = false, require = 0)
    private void xuplus$onSaveData(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean("xuplus_persistent", true);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"), remap = false, require = 0)
    private void xuplus$onReadData(CompoundTag tag, CallbackInfo ci) {
        // C4 already has isAlwaysTicking returning true,
        // this just ensures it persists across save/load
    }
}

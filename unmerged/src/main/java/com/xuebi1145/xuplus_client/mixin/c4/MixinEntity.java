package com.xuebi1145.xuplus_client.mixin.c4;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {
    
    @Inject(method = "shouldStopUpdating", at = @At("HEAD"), cancellable = true, require = 0)
    private void xuplus$onShouldStopUpdating(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity)(Object)this;
        if (entity.getClass().getName().equals("com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity")) {
            cir.setReturnValue(false);
        }
    }
    
    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true, require = 0)
    private void xuplus$onCheckDespawn(CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;
        if (entity.getClass().getName().equals("com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity")) {
            ci.cancel();
        }
    }
}

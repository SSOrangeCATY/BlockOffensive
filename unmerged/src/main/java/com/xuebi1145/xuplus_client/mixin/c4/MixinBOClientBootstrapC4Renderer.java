package com.xuebi1145.xuplus_client.mixin.c4;

import com.phasetranscrystal.blockoffensive.client.BOClientBootstrap;
import com.phasetranscrystal.blockoffensive.entity.BOEntityRegister;
import com.xuebi1145.xuplus_client.renderer.c4.FlatC4Renderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BOClientBootstrap.class, remap = false)
public class MixinBOClientBootstrapC4Renderer {
    @Inject(method = "onRegisterEntityRenderEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void xuplus$registerFlatC4Renderer(EntityRenderersEvent.RegisterRenderers event, CallbackInfo ci) {
        event.registerEntityRenderer(BOEntityRegister.C4.get(), FlatC4Renderer::new);
        ci.cancel();
    }
}
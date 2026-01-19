package com.phasetranscrystal.blockoffensive.mixin.ammo;


import com.phasetranscrystal.blockoffensive.compat.IPassThroughEntity;
import com.phasetranscrystal.blockoffensive.mixin.accessor.BOClipContextAccessor;
import com.tacz.guns.util.block.BlockRayTrace;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = BlockRayTrace.class,remap = false)
public class DefaultAmmoMixin {
    @Inject(
            method = "rayTraceBlocks",
            at = @At(
                    value = "RETURN"
            )
    )
    private static void blockoffensive$checkPassedWall(Level level, ClipContext context, CallbackInfoReturnable<BlockHitResult> cir) {
        if(cir.getReturnValue() == null){
            BOClipContextAccessor accessor = (BOClipContextAccessor) context;
            if (!(accessor.getCollisionContext() instanceof EntityCollisionContext entityCollisionContext)) return;
            if (!(entityCollisionContext.getEntity() instanceof IPassThroughEntity entity)) return;
            entity.blockoffensive$setThroughWall(true);
        }
    }
}

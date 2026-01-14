package com.phasetranscrystal.blockoffensive.mixin;

import com.phasetranscrystal.blockoffensive.compat.IPassThroughEntity;
import com.phasetranscrystal.fpsmatch.common.entity.throwable.SmokeShellEntity;
import com.phasetranscrystal.fpsmatch.compat.CounterStrikeGrenadesCompat;
import com.phasetranscrystal.fpsmatch.compat.LrtacticalCompat;
import com.phasetranscrystal.fpsmatch.compat.impl.FPSMImpl;
import com.phasetranscrystal.fpsmatch.util.BlockRayTraceReflector;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = EntityKineticBullet.class,remap = false)
public abstract class AmmoEntityMixin implements IPassThroughEntity {

    @Unique
    private boolean blockoffensive$passedThroughWall = false;

    @Unique
    private boolean blockoffensive$passedThroughSmoke = false;

    @Redirect(
            method = "onBulletTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/util/block/BlockRayTrace;rayTraceBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"
            )
    )
    private BlockHitResult blockoffensive$checkPassedWall(Level level, ClipContext context) {
        return blockoffensive$rayTraceBlocks(level,context);
    }

    @Inject(
            method = "onBulletTick",
            at = @At(
                    value = "TAIL"
            )
    )
    private void blockoffensive$checkPassedSmoke(CallbackInfo ci) {
        EntityKineticBullet bullet = (EntityKineticBullet)(Object)this;
        Level level = bullet.level();
        if(level.isClientSide()) return;
        if(blockoffensive$passedThroughSmoke) return;
        AABB checker = bullet.getBoundingBox().inflate(16D);
        Vec3 pos = bullet.position();
        if(FPSMImpl.findCounterStrikeGrenadesMod()){
            if(CounterStrikeGrenadesCompat.isInSmokeGrenadeArea(level,checker,pos)){
                blockoffensive$passedThroughSmoke = true;
                return;
            }
        }

        if(FPSMImpl.findLrtacticalMod()){
            if(LrtacticalCompat.isInSmokeGrenadeArea(level,checker,pos)){
                blockoffensive$passedThroughSmoke = true;
                return;
            }
        }

        if(blockoffensive$isPassedSmoke(level,checker,pos)){
            blockoffensive$passedThroughSmoke = true;
        }
    }

    @Unique
    public boolean blockoffensive$isPassedSmoke(Level level, AABB checker, Vec3 position) {
        List<SmokeShellEntity> s1 = level.getEntitiesOfClass(SmokeShellEntity.class,checker);
        for (SmokeShellEntity smoke : s1) {
            if(smoke.isInSmokeArea(position)) {
                return true;
            }
        }
        return false;
    }


    @Unique
    public BlockHitResult blockoffensive$rayTraceBlocks(Level level, ClipContext context) {
        return BlockRayTraceReflector.performRayTrace(context, (rayTraceContext, blockPos) -> {
            BlockState blockState = level.getBlockState(blockPos);
            // 这里添加判断方块是否可以穿透，如果可以穿透则返回 null
            List<String> ids = AmmoConfig.PASS_THROUGH_BLOCKS.get();
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockState.getBlock());
            if (blockId != null && ids.contains(blockId.toString())) {
                blockoffensive$passedThroughWall = true;
                return null;
            }
            // tag
            if (BlockRayTraceReflector.IGNORES.test(blockState)) {
                blockoffensive$passedThroughWall = true;
                return null;
            }
            return BlockRayTraceReflector.getBlockHitResult(level, rayTraceContext, blockPos, blockState);
        }, (rayTraceContext) -> {
            Vec3 vec3 = rayTraceContext.getFrom().subtract(rayTraceContext.getTo());
            return BlockHitResult.miss(rayTraceContext.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(rayTraceContext.getTo()));
        });
    }


    @Override
    public boolean blockoffensive$isWall() {
        return this.blockoffensive$passedThroughWall;
    }

    @Override
    public boolean blockoffensive$isSmoke() {
        return this.blockoffensive$passedThroughSmoke;
    }
}
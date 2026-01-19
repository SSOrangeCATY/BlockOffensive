package com.phasetranscrystal.blockoffensive.mixin.ammo;

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
import net.minecraft.world.entity.Entity;
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

    @Inject(
            method = "onBulletTick",
            at = @At(
                    value = "HEAD"
            )
    )
    private void blockoffensive$checkPassedSmoke(CallbackInfo ci) {
        EntityKineticBullet bullet = (EntityKineticBullet)(Object)this;
        if(bullet.level().isClientSide()) return;
        if(blockoffensive$passedThroughSmoke) return;
        List<Entity> entities = bullet.level().getEntities(bullet, bullet.getBoundingBox().expandTowards(bullet.getDeltaMovement()).inflate(16.0));
        if(entities.isEmpty()) return;
        AABB checker = bullet.getBoundingBox().expandTowards(bullet.getDeltaMovement()).inflate(1D);

        if(FPSMImpl.findCounterStrikeGrenadesMod()){
            if(CounterStrikeGrenadesCompat.isInSmokeGrenadeArea(entities,checker)){
                blockoffensive$passedThroughSmoke = true;
                return;
            }
        }

        if(FPSMImpl.findLrtacticalMod()){
            if(LrtacticalCompat.isInSmokeGrenadeArea(entities,checker)){
                blockoffensive$passedThroughSmoke = true;
                return;
            }
        }

        if(blockoffensive$isPassedSmoke(entities,checker)){
            blockoffensive$passedThroughSmoke = true;
        }
    }

    @Unique
    private boolean blockoffensive$isPassedSmoke(List<Entity> entities, AABB checker) {
        List<SmokeShellEntity> smokes = entities.stream()
                .filter(entity -> entity instanceof SmokeShellEntity)
                .map(entity -> (SmokeShellEntity)entity)
                .toList();

        for (SmokeShellEntity smoke : smokes) {
            if(smoke.isInSmokeArea(checker)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean blockoffensive$isWall() {
        return this.blockoffensive$passedThroughWall;
    }

    @Override
    public void blockoffensive$setThroughWall(boolean passed){
        this.blockoffensive$passedThroughWall = passed;
    }

    @Override
    public boolean blockoffensive$isSmoke() {
        return this.blockoffensive$passedThroughSmoke;
    }

    @Override
    public void blockoffensive$setThroughSmoke(boolean passed){
        this.blockoffensive$passedThroughSmoke = passed;
    }
}
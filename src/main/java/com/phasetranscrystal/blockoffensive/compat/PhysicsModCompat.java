package com.phasetranscrystal.blockoffensive.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.diebuddies.config.ConfigMobs;
import net.diebuddies.physics.PhysicsMod;
import net.diebuddies.physics.PhysicsWorld;
import net.diebuddies.physics.ragdoll.Ragdoll;
import net.diebuddies.physics.settings.mobs.MobPhysicsType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class PhysicsModCompat {

    public static void handleDead(int EntityId) {
        ClientLevel world = Minecraft.getInstance().level;
        if(world == null) return;
        Entity entity = world.getEntity(EntityId);
        if (entity instanceof LivingEntity living && RenderSystem.isOnRenderThread() && ConfigMobs.getMobSetting(entity).getType() != MobPhysicsType.OFF) {
            PhysicsMod.blockifyEntity(living.getCommandSenderWorld(), living);
            frozenAll();
        }
    }

    public static void frozenAll(){
        for(PhysicsMod physicsMod : PhysicsMod.instances.values()){
            PhysicsWorld world = physicsMod.getPhysicsWorld();
            for (Ragdoll ragdoll : world.getRagdolls()) {
                ragdoll.setFrozen(true);
            }
        }
    }

    public static void reset() {
        for(PhysicsMod physicsMod : PhysicsMod.instances.values()){
            PhysicsWorld world = physicsMod.getPhysicsWorld();
            for (Ragdoll ragdoll : world.getRagdolls()) {
                world.removeRagdoll(ragdoll);
            }
        }
    }

}

package com.phasetranscrystal.blockoffensive.compat;

import com.mojang.blaze3d.systems.RenderSystem;

import net.diebuddies.config.ConfigMobs;
import net.diebuddies.physics.PhysicsMod;
import net.diebuddies.physics.PhysicsWorld;
import net.diebuddies.physics.ragdoll.Ragdoll;
import net.diebuddies.physics.settings.mobs.MobPhysicsType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public class PhysicsModCompat {
    @OnlyIn(Dist.CLIENT)
    public static void handleDead() {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player != null && RenderSystem.isOnRenderThread() && ConfigMobs.getMobSetting(player).getType() != MobPhysicsType.OFF) {
            PhysicsMod.blockifyEntity(player.getCommandSenderWorld(), player);
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

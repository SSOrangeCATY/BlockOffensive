package com.phasetranscrystal.blockoffensive.compat;

import com.mojang.blaze3d.systems.RenderSystem;

import net.diebuddies.config.ConfigMobs;
import net.diebuddies.physics.PhysicsMod;
import net.diebuddies.physics.settings.mobs.MobPhysicsType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;


public class PhysicsModCompat {
    @OnlyIn(Dist.CLIENT)
    public static void handleDead(UUID uuid) {
        ClientLevel level = Minecraft.getInstance().level;
        if(level == null) return;
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if(player == null) return;
        if(!uuid.equals(player.getUUID())){
            for (Entity entity : level.entitiesForRendering()) {
                if (entity.getUUID().equals(player.getUUID()) && entity instanceof AbstractClientPlayer clientPlayer) {
                    player = clientPlayer;
                    break;
                }
            }
        }

        if (RenderSystem.isOnRenderThread() && ConfigMobs.getMobSetting(player).getType() != MobPhysicsType.OFF) {
            PhysicsMod.blockifyEntity(player.getCommandSenderWorld(), player);
        }
    }


    public static void clearRagdoll() {
        PhysicsMod physicsMod = PhysicsMod.getInstance(Minecraft.getInstance().level);
        physicsMod.ragdolls.clear();
        physicsMod.blockifiedEntity.clear();
        physicsMod.blockifyEntity = null;
        physicsMod.sodiumRemoveRagdolls.clear();
    }

}

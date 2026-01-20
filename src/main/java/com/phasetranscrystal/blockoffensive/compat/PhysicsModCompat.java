package com.phasetranscrystal.blockoffensive.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.phasetranscrystal.blockoffensive.util.BOUtil;
import net.diebuddies.config.ConfigMobs;
import net.diebuddies.math.AABBf;
import net.diebuddies.physics.IRigidBody;
import net.diebuddies.physics.PhysicsEntity;
import net.diebuddies.physics.PhysicsMod;
import net.diebuddies.physics.PhysicsWorld;
import net.diebuddies.physics.ragdoll.BreakableRagdoll;
import net.diebuddies.physics.ragdoll.Ragdoll;
import net.diebuddies.physics.ragdoll.RagdollHook;
import net.diebuddies.physics.ragdoll.RagdollMapper;
import net.diebuddies.physics.settings.mobs.MobPhysicsType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3f;
import physx.common.PxVec3;
import physx.physics.PxRigidDynamic;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class PhysicsModCompat {

    public static void init(){
        RagdollMapper.addHook(BORagdollHook.INSTANCE);
        MinecraftForge.EVENT_BUS.register(BORagdollHook.class);
    }

    public static void handleDead(int EntityId) {
        ClientLevel world = Minecraft.getInstance().level;
        if(world == null) return;
        Entity entity = world.getEntity(EntityId);
        if (entity instanceof LivingEntity living && RenderSystem.isOnRenderThread() && ConfigMobs.getMobSetting(entity).getType() != MobPhysicsType.OFF) {
            PhysicsMod.blockifyEntity(living.getCommandSenderWorld(), living);
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
        BORagdollHook.RAGDOLL_MAP.clear();
    }

    public static void remove(Ragdoll ragdoll) {
        for(PhysicsMod physicsMod : PhysicsMod.instances.values()){
            PhysicsWorld world = physicsMod.getPhysicsWorld();
            world.removeRagdoll(ragdoll);
        }
    }

    public static class BORagdollHook implements RagdollHook {

        public static final Map<UUID,HookData> RAGDOLL_MAP = new ConcurrentHashMap<>();

        public static final BORagdollHook INSTANCE = new BORagdollHook();

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event){
            if(event.phase == TickEvent.Phase.END){
                INSTANCE.tick();
            }
        }

        @Override
        public void map(Ragdoll ragdoll, Entity entity, EntityModel entityModel) {
            if(entity instanceof Player player){
                RAGDOLL_MAP.put(player.getUUID(),new HookData(ragdoll));
            }
        }

        public void remove(UUID uuid){
            if(RAGDOLL_MAP.containsKey(uuid)){
                PhysicsModCompat.remove(RAGDOLL_MAP.get(uuid).getRagdoll());
            }
            del(uuid);
        }

        private void del(UUID uuid){
            RAGDOLL_MAP.remove(uuid);
        }

        public void tick() {
            if (RAGDOLL_MAP.isEmpty()) return;

            for (Map.Entry<UUID, HookData> entry : RAGDOLL_MAP.entrySet()) {
                HookData data = entry.getValue();
                Ragdoll ragdoll = data.getRagdoll();
                if(ragdoll.btBodies.isEmpty()){
                    continue;
                }

                if (ragdoll.isFrozen()) {
                    continue;
                }

                data.tick();

                int sleepingCount = 0;
                int totalDynamicBodies = 0;

                for (IRigidBody body : ragdoll.btBodies) {
                    if (body.getRigidBody() instanceof PxRigidDynamic dynamicBody) {
                        totalDynamicBodies++;
                        if(body.hasTransformationChanged()){
                            if (data.livingTicks >= 20){
                                Matrix4d current = body.getEntity().getTransformation();
                                Matrix4d old = body.getEntity().getOldTransformation();

                                Vector3d currentPos = new Vector3d();
                                Vector3d oldPos = new Vector3d();
                                current.getTranslation(currentPos);
                                old.getTranslation(oldPos);

                                double positionChange = currentPos.distance(oldPos);

                                if (positionChange < 0.005f) {
                                    sleepingCount++;
                                } else {
                                    sleepingCount = 0;
                                    break;
                                }
                            }
                        } else {
                            if (dynamicBody.isSleeping()) {
                                sleepingCount++;
                            }
                        }
                    }
                }

                if (totalDynamicBodies > 0 && sleepingCount == totalDynamicBodies) {
                    try{
                        ragdoll.setFrozen(true);
                    }catch (Exception ignored){
                    }
                }
            }
        }

        @Override
        public void filterCuboidsFromEntities(List<PhysicsEntity> list, Entity entity, EntityModel entityModel) {

        }

        public static class HookData{
            public int livingTicks = 0;
            public final Ragdoll ragdoll;


            public HookData(Ragdoll ragdoll) {
                this.ragdoll = ragdoll;
            }

            public void tick(){
                livingTicks++;
            }

            public int getLivingTicks() {
                return livingTicks;
            }

            public Ragdoll getRagdoll() {
                return ragdoll;
            }
        }
    }

}
package com.phasetranscrystal.blockoffensive.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.phasetranscrystal.blockoffensive.util.BOUtil;
import net.diebuddies.config.ConfigMobs;
import net.diebuddies.physics.PhysicsEntity;
import net.diebuddies.physics.PhysicsMod;
import net.diebuddies.physics.PhysicsWorld;
import net.diebuddies.physics.ragdoll.Ragdoll;
import net.diebuddies.physics.ragdoll.RagdollHook;
import net.diebuddies.physics.ragdoll.RagdollMapper;
import net.diebuddies.physics.settings.mobs.MobPhysicsType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

        public static final Map<UUID,Ragdoll> RAGDOLL_MAP = new ConcurrentHashMap<>();

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
                RAGDOLL_MAP.put(player.getUUID(),ragdoll);
            }
        }

        public void remove(UUID uuid){
            if(RAGDOLL_MAP.containsKey(uuid)){
                PhysicsModCompat.remove(RAGDOLL_MAP.get(uuid));
            }
            RAGDOLL_MAP.remove(uuid);
        }

        public void tick(){
            if(RAGDOLL_MAP.isEmpty()) return;

            for (Ragdoll ragdoll : RAGDOLL_MAP.values()) {
                if(!ragdoll.isFrozen() && BOUtil.isFrozen(ragdoll.velocity)){
                    ragdoll.setFrozen(true);
                }
            }
        }

        @Override
        public void filterCuboidsFromEntities(List<PhysicsEntity> list, Entity entity, EntityModel entityModel) {

        }
    }

}

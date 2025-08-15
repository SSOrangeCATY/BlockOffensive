package com.phasetranscrystal.blockoffensive.compat;

import net.diebuddies.physics.PhysicsMod;
import net.diebuddies.physics.PhysicsWorld;
import net.diebuddies.physics.ragdoll.Ragdoll;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PhysicsModCompat {

    public static void reset() {
        for(PhysicsMod physicsMod : PhysicsMod.instances.values()){
            PhysicsWorld world = physicsMod.getPhysicsWorld();
            for (Ragdoll ragdoll : world.getRagdolls()) {
                world.removeRagdoll(ragdoll);
            }
        }
    }

}

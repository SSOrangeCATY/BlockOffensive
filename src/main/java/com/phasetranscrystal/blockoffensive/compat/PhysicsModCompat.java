package com.phasetranscrystal.blockoffensive.compat;

import java.util.UUID;

public class PhysicsModCompat {
    public static void init() {
    }

    public static void handleDead(int entityId) {
    }

    public static void frozenAll() {
    }

    public static void reset() {
    }

    public static class BORagdollHook {
        public static final BORagdollHook INSTANCE = new BORagdollHook();

        public void remove(UUID uuid) {
        }
    }
}

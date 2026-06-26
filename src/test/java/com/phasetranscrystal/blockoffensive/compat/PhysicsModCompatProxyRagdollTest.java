package com.phasetranscrystal.blockoffensive.compat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhysicsModCompatProxyRagdollTest {

    private static final int ENTITY_ID = 4242;
    private final ProxiedRagdollRegistry registry = new ProxiedRagdollRegistry();

    @AfterEach
    void tearDown() {
        registry.clear(ENTITY_ID);
    }

    @Test
    void proxiedRagdollBeginsOnlyOnceUntilCleared() {
        assertTrue(registry.begin(ENTITY_ID));
        assertFalse(registry.begin(ENTITY_ID));

        registry.clear(ENTITY_ID);

        assertTrue(registry.begin(ENTITY_ID));
    }

    @Test
    void clearingAllAllowsProxyRagdollToBeginAgain() {
        assertTrue(registry.begin(ENTITY_ID));
        assertFalse(registry.begin(ENTITY_ID));

        registry.clearAll();

        assertTrue(registry.begin(ENTITY_ID));
    }
}

package com.phasetranscrystal.blockoffensive.compat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void invisibleEntityKeepsProxyRagdollPendingInsteadOfSucceeding() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/compat/PhysicsModCompat.java"));

        assertTrue(source.contains("private static final int RAGDOLL_RETRY_TICKS = 60;"));
        assertTrue(source.contains("PENDING_DEATHS.put(entityId, RAGDOLL_RETRY_TICKS);"));
        assertTrue(source.contains("if (!entity.isInvisible()) {"));
        assertTrue(source.contains("} else {\r\n                        return false;")
                || source.contains("} else {\n                        return false;"));
        assertFalse(source.contains("} else {\r\n                        clearProxiedRagdoll(EntityId);")
                || source.contains("} else {\n                        clearProxiedRagdoll(EntityId);"));
    }
}

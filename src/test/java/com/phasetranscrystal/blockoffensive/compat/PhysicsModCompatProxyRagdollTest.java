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
    void invisibleEntityIsTemporarilyRenderableForProxyRagdoll() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/compat/PhysicsModCompat.java"));

        assertTrue(source.contains("private static final int RAGDOLL_RETRY_TICKS = 60;"));
        assertTrue(source.contains("PENDING_DEATHS.put(entityId, RAGDOLL_RETRY_TICKS);"));
        assertTrue(source.contains("boolean wasInvisible = entity.isInvisible();"));
        assertTrue(source.contains("entity.setInvisible(false);"));
        assertTrue(source.contains("entity.setInvisible(true);"));
        assertTrue(source.contains("finally {"));
        assertFalse(source.contains("} else {\r\n                        clearProxiedRagdoll(EntityId);")
                || source.contains("} else {\n                        clearProxiedRagdoll(EntityId);"));
        assertFalse(source.contains("} else {\r\n                        return false;")
                || source.contains("} else {\n                        return false;"));
    }

    @Test
    void proxyRagdollOnlyCompletesAfterCorpseIsGenerated() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/compat/PhysicsModCompat.java"));

        assertTrue(source.contains("hasUsableMappedPlayerRagdoll(entity, EntityId)"));
        assertTrue(source.contains("mod.alreadyBlockified.remove(entity.getId());"));
        assertTrue(source.contains("debugRagdoll(\"retry stale already-blockified player entity={}\""));
        assertTrue(source.contains("boolean corpseGenerated = false;"));
        assertTrue(source.contains("corpseGenerated = blockifiedPartCount > 0;"));
        assertTrue(source.contains("corpseGenerated = isGeneratedRagdollUsable(entity, EntityId, ragdoll);"));
        assertTrue(source.contains("if (corpseGenerated) {"));
        assertTrue(source.contains("rollbackFailedDeadAttempt(mod, entity, EntityId, createdRagdoll);"));
        assertTrue(source.contains("debugRagdoll(\"retry no corpse entity={}"));
    }

    @Test
    void failedProxyRagdollAttemptRollsBackPhysicsModStateForRetry() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/phasetranscrystal/blockoffensive/compat/PhysicsModCompat.java"));

        assertTrue(source.contains("private static boolean isGeneratedRagdollUsable"));
        assertTrue(source.contains("private static boolean isUsableRagdoll"));
        assertTrue(source.contains("return ragdoll != null;"));
        assertTrue(source.contains("private static boolean hasMappedPlayerRagdoll"));
        assertTrue(source.contains("private static void rollbackFailedDeadAttempt"));
        assertFalse(source.contains("PhysicsModCompat.remove(createdRagdoll);"));
        assertTrue(source.contains("removeUnmappedPlayerRagdoll(entity, entityId);"));
        assertTrue(source.contains("BORagdollHook.RAGDOLL_MAP.remove(player.getUUID());"));
    }
}

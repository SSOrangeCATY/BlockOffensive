package com.phasetranscrystal.blockoffensive.data;

import java.util.Objects;
import java.util.UUID;

public final class DeathMessageRules {
    private DeathMessageRules() {
    }

    public static boolean resolveNoScopeFlag(boolean isScopedKill, boolean attackerIsDeadPlayer) {
        return isScopedKill && !attackerIsDeadPlayer;
    }

    public static boolean hasDistinctAssist(UUID assistUUID, UUID killerUUID) {
        return assistUUID != null && !Objects.equals(assistUUID, killerUUID);
    }

    public static boolean isSuicide(UUID deadUUID, UUID killerUUID) {
        return Objects.equals(deadUUID, killerUUID);
    }
}

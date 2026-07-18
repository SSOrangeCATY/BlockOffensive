package com.phasetranscrystal.blockoffensive.minimap;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CSMinimapVisibilityEventBridgeTest {
    @Test
    void resetClearsIntelAndDeathRemoveWorks() {
        CSMinimapIntelLedger ledger = new CSMinimapIntelLedger(CSMinimapVisibilityConfig.DEFAULTS);
        CSMinimapVisibilityEventBridge bridge = new CSMinimapVisibilityEventBridge(ledger);
        UUID enemy = UUID.fromString("33333333-3333-3333-3333-333333333333");
        bridge.onLineOfSight("ct", enemy, 1, 0, 0, 0, 0f, Optional.empty());
        assertTrue(ledger.get("ct", enemy).isPresent());
        bridge.onTargetDeathOrLeave(enemy);
        assertTrue(ledger.get("ct", enemy).isEmpty());
        bridge.onFireExposure("ct", enemy, 2, 1, 1, 1, 0f, Optional.empty());
        bridge.onRoundOrMapReset();
        assertTrue(ledger.get("ct", enemy).isEmpty());
    }
}
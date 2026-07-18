package com.phasetranscrystal.blockoffensive.minimap;

import com.phasetranscrystal.fpsmatch.core.minimap.marker.MarkerCandidate;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.MarkerSnapshot;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.MinimapViewerContext;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.PlayerPoseSnapshot;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.ViewerRole;
import com.phasetranscrystal.fpsmatch.core.minimap.model.NamespacedId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSMinimapVisibilityPolicyTest {
    private static final UUID SELF_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEAMMATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ENEMY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final NamespacedId SELF = PlayerPoseSnapshot.markerIdFor(SELF_ID);
    private static final NamespacedId TEAMMATE = PlayerPoseSnapshot.markerIdFor(TEAMMATE_ID);
    private static final NamespacedId ENEMY = PlayerPoseSnapshot.markerIdFor(ENEMY_ID);
    private static final NamespacedId DEATH = NamespacedId.parse("fpsmatch:event/death/" + SELF_ID);
    private static final NamespacedId TYPE_PLAYER = PlayerPoseSnapshot.TYPE_ID;
    private static final NamespacedId TYPE_DEATH = NamespacedId.parse("fpsmatch:type/death");
    private static final NamespacedId STYLE = NamespacedId.parse("fpsmatch:style/default");

    @Test
    void teammateAndSelfVisibleEnemyHiddenByDefault() {
        CSMinimapIntelLedger ledger = new CSMinimapIntelLedger(CSMinimapVisibilityConfig.DEFAULTS);
        CSMinimapVisibilityPolicy policy = new CSMinimapVisibilityPolicy(ledger);
        MinimapViewerContext ctx = new MinimapViewerContext(ViewerRole.ACTIVE_PLAYER, "ct", Optional.of(SELF), true, false);
        List<MarkerSnapshot.Marker> visible = policy.filter(ctx, List.of(
                living(SELF, "ct"),
                living(TEAMMATE, "ct"),
                living(ENEMY, "t")
        ));
        assertTrue(visible.stream().anyMatch(m -> m.markerId().equals(SELF)));
        assertTrue(visible.stream().anyMatch(m -> m.markerId().equals(TEAMMATE)));
        assertFalse(visible.stream().anyMatch(m -> m.markerId().equals(ENEMY)));
    }

    @Test
    void losAndFireRevealEnemyThenLastKnownThenExpire() {
        CSMinimapVisibilityConfig config = new CSMinimapVisibilityConfig(10, 20, 30, true);
        CSMinimapIntelLedger ledger = new CSMinimapIntelLedger(config);
        CSMinimapVisibilityPolicy policy = new CSMinimapVisibilityPolicy(ledger, config);
        MinimapViewerContext ctx = new MinimapViewerContext(ViewerRole.ACTIVE_PLAYER, "ct", Optional.of(SELF), true, false);

        ledger.noteLos("ct", ENEMY_ID, 100, 5, 64, 6, 90f, Optional.of("ground"));
        // active until 110 inclusive
        ledger.tick(110);
        assertEquals(EnemyIntelState.Mode.ACTIVE, ledger.get("ct", ENEMY_ID).orElseThrow().mode());
        assertTrue(policy.filter(ctx, List.of(living(SELF, "ct"))).stream().anyMatch(m -> m.markerId().equals(ENEMY)));

        ledger.tick(111);
        assertEquals(EnemyIntelState.Mode.LAST_KNOWN, ledger.get("ct", ENEMY_ID).orElseThrow().mode());
        assertTrue(policy.filter(ctx, List.of(living(SELF, "ct"))).stream().anyMatch(m -> m.markerId().equals(ENEMY)));

        ledger.noteFire("ct", ENEMY_ID, 120, 8, 64, 9, 10f, Optional.of("ground"));
        assertEquals(EnemyIntelState.Mode.ACTIVE, ledger.get("ct", ENEMY_ID).orElseThrow().mode());

        ledger.tick(200);
        assertTrue(ledger.get("ct", ENEMY_ID).isEmpty());
        assertFalse(policy.filter(ctx, List.of(living(SELF, "ct"))).stream().anyMatch(m -> m.markerId().equals(ENEMY)));
    }

    @Test
    void deathRemovesIntelAndDeadMemberSeesTeamSharedIntel() {
        CSMinimapIntelLedger ledger = new CSMinimapIntelLedger(CSMinimapVisibilityConfig.DEFAULTS);
        ledger.noteLos("ct", ENEMY_ID, 10, 1, 1, 1, 0f, Optional.empty());
        ledger.removeTarget(ENEMY_ID);
        assertTrue(ledger.get("ct", ENEMY_ID).isEmpty());

        ledger.noteFire("ct", ENEMY_ID, 20, 2, 2, 2, 0f, Optional.empty());
        CSMinimapVisibilityPolicy policy = new CSMinimapVisibilityPolicy(ledger);
        MinimapViewerContext dead = new MinimapViewerContext(ViewerRole.DEAD_TEAM_MEMBER, "ct", Optional.of(SELF), true, false);
        assertTrue(policy.filter(dead, List.of(death(DEATH, "ct"))).stream().anyMatch(m -> m.markerId().equals(DEATH)));
        assertTrue(policy.filter(dead, List.of()).stream().anyMatch(m -> m.markerId().equals(ENEMY)));
    }

    @Test
    void ffaOnlySelfAndOwnDeathNoAlliesNoIntel() {
        CSMinimapIntelLedger ledger = new CSMinimapIntelLedger(CSMinimapVisibilityConfig.DEFAULTS);
        ledger.noteLos("1", ENEMY_ID, 1, 0, 0, 0, 0f, Optional.empty());
        CSMinimapVisibilityPolicy policy = new CSMinimapVisibilityPolicy(ledger);
        MinimapViewerContext ffa = new MinimapViewerContext(ViewerRole.ACTIVE_PLAYER, "1", Optional.of(SELF), false, false);
        List<MarkerSnapshot.Marker> visible = policy.filter(ffa, List.of(
                living(SELF, "1"),
                living(TEAMMATE, "1"),
                living(ENEMY, "2"),
                death(DEATH, "1")
        ));
        assertEquals(2, visible.size());
        assertTrue(visible.stream().anyMatch(m -> m.markerId().equals(SELF)));
        assertTrue(visible.stream().anyMatch(m -> m.markerId().equals(DEATH)));
        assertFalse(visible.stream().anyMatch(m -> m.markerId().equals(TEAMMATE)));
        assertFalse(visible.stream().anyMatch(m -> m.markerId().equals(ENEMY)));
    }

    @Test
    void protectedEnemyCandidateNotSerializedWithoutIntel() {
        CSMinimapVisibilityPolicy policy = new CSMinimapVisibilityPolicy(new CSMinimapIntelLedger(CSMinimapVisibilityConfig.DEFAULTS));
        MinimapViewerContext active = new MinimapViewerContext(ViewerRole.ACTIVE_PLAYER, "ct", Optional.of(SELF), true, false);
        List<MarkerSnapshot.Marker> visible = policy.filter(active, List.of(living(ENEMY, "t")));
        assertTrue(visible.isEmpty());
        assertFalse(visible.stream().anyMatch(m -> m.markerId().equals(ENEMY)));
    }

    private static MarkerCandidate living(NamespacedId id, String team) {
        return new MarkerCandidate(id, TYPE_PLAYER, STYLE, 0, 0, 0, 0f, 1, Optional.empty(), Optional.empty(), team, false, false);
    }

    private static MarkerCandidate death(NamespacedId id, String team) {
        return new MarkerCandidate(id, TYPE_DEATH, STYLE, 0, 0, 0, 0f, 1, Optional.of(50L), Optional.empty(), team, true, false);
    }
}
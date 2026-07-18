package com.phasetranscrystal.blockoffensive.minimap;

import com.phasetranscrystal.fpsmatch.core.minimap.marker.DeathMarkerLedger;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.MarkerCandidate;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.MarkerSnapshot;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.MarkerStreamManager;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.MarkerStreamUpdate;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.MinimapViewerContext;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.PlayerPoseSnapshot;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.ViewerRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure integration of Gate 4 BO providers/policies/trackers for CS/CSDM lifecycle matrices.
 */
class CSMinimapLifecycleIntegrationTest {
    private static final UUID T1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CT1 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DROP = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID C4E = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    void csDeathCaptureAndTeamSwitchResetsMarkerEpoch() {
        DeathMarkerLedger deaths = new DeathMarkerLedger();
        AtomicLong tick = new AtomicLong(10);
        AtomicReference<List<PlayerPoseSnapshot>> living = new AtomicReference<>(List.of(pose(T1, "t", true)));
        CSMapMinimapMarkerProvider players = new CSMapMinimapMarkerProvider(living::get, deaths, tick::get);
        CSMinimapIntelLedger intel = new CSMinimapIntelLedger(CSMinimapVisibilityConfig.DEFAULTS);
        CSMinimapVisibilityPolicy policy = new CSMinimapVisibilityPolicy(intel);
        MarkerStreamManager stream = new MarkerStreamManager(players, policy);

        MinimapViewerContext tViewer = new MinimapViewerContext(
                ViewerRole.ACTIVE_PLAYER, "t", Optional.of(PlayerPoseSnapshot.markerIdFor(T1)), true, false
        );
        MarkerStreamUpdate first = stream.subscribe(tViewer, 10);
        assertEquals(MarkerStreamUpdate.Kind.RESET, first.kind());
        UUID epoch1 = first.streamEpoch();

        CSDeathMarkerCapture.captureFromWorldPose(deaths, T1, "t", 1, 2, 3, 0f, 20, 100, Optional.empty());
        living.set(List.of());
        tick.set(21);
        MinimapViewerContext deadT = new MinimapViewerContext(
                ViewerRole.DEAD_TEAM_MEMBER, "t", Optional.empty(), true, false
        );
        List<MarkerCandidate> deathCandidates = players.collect(deadT);
        assertTrue(deathCandidates.stream().anyMatch(MarkerCandidate::deathEvent));

        MarkerStreamUpdate afterDeathRole = stream.tick(deadT, 21);
        assertEquals(MarkerStreamUpdate.Kind.RESET, afterDeathRole.kind());
        assertNotEquals(epoch1, afterDeathRole.streamEpoch());

        MinimapViewerContext afterSwitch = new MinimapViewerContext(
                ViewerRole.ACTIVE_PLAYER, "ct", Optional.of(PlayerPoseSnapshot.markerIdFor(T1)), true, false
        );
        MarkerStreamUpdate afterTeam = stream.tick(afterSwitch, 30);
        assertEquals(MarkerStreamUpdate.Kind.RESET, afterTeam.kind());
        assertEquals(0L, afterTeam.sequence());
        assertNotEquals(afterDeathRole.streamEpoch(), afterTeam.streamEpoch());
    }

    @Test
    void c4LifecycleVisibilityMatrixWithObjectiveProvider() {
        CSGameObjectiveTracker tracker = new CSGameObjectiveTracker();
        AtomicLong tick = new AtomicLong(100);
        AtomicReference<Optional<Boolean>> ctIntel = new AtomicReference<>(Optional.of(false));
        CSGameObjectiveMarkerProvider provider = new CSGameObjectiveMarkerProvider(
                tracker::snapshot, tick::get, ctIntel::get
        );
        MinimapViewerContext t = new MinimapViewerContext(ViewerRole.ACTIVE_PLAYER, "t", Optional.empty(), true, false);
        MinimapViewerContext ct = new MinimapViewerContext(ViewerRole.ACTIVE_PLAYER, "ct", Optional.empty(), true, false);
        MinimapViewerContext observer = new MinimapViewerContext(ViewerRole.SPECTATOR_TEAM, "observer", Optional.empty(), false, true);

        tracker.assignCarrier(T1, 100, 0, 64, 0, 0f, Optional.empty());
        assertFalse(provider.collect(t).isEmpty());
        assertTrue(provider.collect(ct).isEmpty());
        assertFalse(provider.collect(observer).isEmpty());

        tracker.forceDrop(9, DROP, 110, 1, 64, 1, 0f, Optional.empty());
        assertFalse(provider.collect(t).isEmpty());
        assertTrue(provider.collect(ct).isEmpty());
        tracker.noteDroppedDiscoveredByCt(111, 1, 64, 1, 0f, Optional.empty());
        assertFalse(provider.collect(ct).isEmpty());

        tracker.successfulPickup(T1, 120, 2, 64, 2, 0f, Optional.empty());
        ctIntel.set(Optional.of(true));
        assertFalse(provider.collect(ct).isEmpty());

        tracker.planted(200, C4E, 130, 5, 64, 5, 0f, Optional.empty(), Optional.of("site_1"));
        assertFalse(provider.collect(t).isEmpty());
        assertFalse(provider.collect(ct).isEmpty());
        tracker.startDefusing(CT1, 140, 0.4f);
        assertEquals(CSGameObjectiveMarkerProvider.STYLE_PLANTED, provider.collect(t).get(0).styleId());
        assertEquals(CSGameObjectiveMarkerProvider.STYLE_DEFUSING, provider.collect(ct).get(0).styleId());
        tracker.defused(150);
        assertTrue(provider.collect(t).isEmpty());
        tracker.roundReset();
        assertEquals(C4ObjectivePhase.NONE, tracker.snapshot().phase());
    }

    @Test
    void csdmFfaSelfOnlyVersusTdmSharedIntel() {
        CSMinimapIntelLedger ledger = new CSMinimapIntelLedger(CSMinimapVisibilityConfig.DEFAULTS);
        ledger.noteLos("1", CT1, 1, 0, 0, 0, 0f, Optional.empty());
        CSMinimapVisibilityPolicy policy = new CSMinimapVisibilityPolicy(ledger);
        MinimapViewerContext ffa = new MinimapViewerContext(
                ViewerRole.ACTIVE_PLAYER, "1", Optional.of(PlayerPoseSnapshot.markerIdFor(T1)), false, false
        );
        List<MarkerSnapshot.Marker> ffaVisible = policy.filter(ffa, List.of(
                livingCandidate(T1, "1"),
                livingCandidate(CT1, "2")
        ));
        assertEquals(1, ffaVisible.size());

        MinimapViewerContext tdm = new MinimapViewerContext(
                ViewerRole.ACTIVE_PLAYER, "alpha", Optional.of(PlayerPoseSnapshot.markerIdFor(T1)), true, false
        );
        List<MarkerSnapshot.Marker> tdmVisible = policy.filter(tdm, List.of(
                livingCandidate(T1, "alpha"),
                livingCandidate(CT1, "alpha")
        ));
        assertEquals(2, tdmVisible.size());
    }

    private static PlayerPoseSnapshot pose(UUID id, String team, boolean living) {
        return new PlayerPoseSnapshot(id, team, 0, 64, 0, 0f, 1, Optional.empty(), living);
    }

    private static MarkerCandidate livingCandidate(UUID id, String team) {
        return pose(id, team, true).toLivingCandidate();
    }
}
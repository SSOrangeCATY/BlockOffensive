package com.phasetranscrystal.blockoffensive.minimap;

import com.phasetranscrystal.fpsmatch.core.minimap.marker.DeathMarkerEvent;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.DeathMarkerLedger;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.MarkerCandidate;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSMapMinimapMarkerProviderTest {
    private static final UUID A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void livingPoseEmitsYawPositionAndFloorHint() {
        DeathMarkerLedger ledger = new DeathMarkerLedger();
        AtomicLong tick = new AtomicLong(10);
        CSMapMinimapMarkerProvider provider = new CSMapMinimapMarkerProvider(
                () -> List.of(new PlayerPoseSnapshot(A, "ct", 1, 64, 2, 90f, 10, Optional.of("ground"), true)),
                ledger,
                tick::get
        );
        MinimapViewerContext ctx = CSMinimapIdentityResolver.resolve(A, "ct", true, false, true, false);
        MarkerCandidate marker = provider.collect(ctx).get(0);
        assertEquals(1.0, marker.x());
        assertEquals(64.0, marker.y());
        assertEquals(2.0, marker.z());
        assertEquals(90f, marker.yaw());
        assertEquals(Optional.of("ground"), marker.floorSlug());
        assertFalse(marker.deathEvent());
    }

    @Test
    void deathCaptureUsesCsAndCsdmTtlAndExpires() {
        DeathMarkerLedger ledger = new DeathMarkerLedger();
        DeathMarkerEvent cs = CSDeathMarkerCapture.captureFromWorldPose(
                ledger, A, "ct", 5, 70, 6, 45f, 100,
                CSMapMinimapMarkerProvider.CS_DEATH_TTL_TICKS, Optional.of("ground")
        );
        assertEquals(200L, cs.expiresTick());
        assertTrue(cs.toCandidate().deathEvent());

        DeathMarkerLedger csdmLedger = new DeathMarkerLedger();
        DeathMarkerEvent csdm = CSDeathMarkerCapture.captureFromWorldPose(
                csdmLedger, B, "1", 0, 0, 0, 0f, 50,
                CSMapMinimapMarkerProvider.CSDM_DEATH_TTL_TICKS, Optional.empty()
        );
        assertEquals(90L, csdm.expiresTick());

        AtomicLong tick = new AtomicLong(201);
        CSMapMinimapMarkerProvider provider = new CSMapMinimapMarkerProvider(
                List::of, ledger, tick::get
        );
        MinimapViewerContext ctx = CSMinimapIdentityResolver.resolve(A, "ct", false, false, true, false);
        assertTrue(provider.collect(ctx).isEmpty());
    }

    @Test
    void identityResetEmitsNewEpochBeforeDeltasOnTeamOrRoleChange() {
        DeathMarkerLedger ledger = new DeathMarkerLedger();
        AtomicReference<List<PlayerPoseSnapshot>> poses = new AtomicReference<>(List.of(
                new PlayerPoseSnapshot(A, "ct", 0, 0, 0, 0f, 1, Optional.empty(), true)
        ));
        AtomicLong tick = new AtomicLong(1);
        CSMapMinimapMarkerProvider provider = new CSMapMinimapMarkerProvider(poses::get, ledger, tick::get);
        MarkerStreamManager stream = new MarkerStreamManager(provider, (context, candidates) ->
                candidates.stream().map(MarkerCandidate::toMarker).toList()
        );

        MinimapViewerContext living = CSMinimapIdentityResolver.resolve(A, "ct", true, false, true, false);
        MarkerStreamUpdate first = stream.subscribe(living, 1);
        assertEquals(MarkerStreamUpdate.Kind.RESET, first.kind());

        MinimapViewerContext dead = CSMinimapIdentityResolver.resolve(A, "ct", false, false, true, false);
        MarkerStreamUpdate reset = stream.tick(dead, 2);
        assertEquals(MarkerStreamUpdate.Kind.RESET, reset.kind());
        assertNotEquals(first.streamEpoch(), reset.streamEpoch());
        assertEquals(0L, reset.sequence());

        MinimapViewerContext switched = CSMinimapIdentityResolver.resolve(A, "t", true, false, true, false);
        MarkerStreamUpdate teamReset = stream.tick(switched, 3);
        assertEquals(MarkerStreamUpdate.Kind.RESET, teamReset.kind());
        assertNotEquals(reset.streamEpoch(), teamReset.streamEpoch());
    }

    @Test
    void ffaDeathMarkerIsSelfOnlyInputWhileLivingHasSelfPose() {
        // Provider emits all candidates; FFA visibility policy (Task 5) filters allies.
        // Here we assert identity shared-intel flag is false for FFA so policy can deny allies.
        assertFalse(CSMinimapIdentityResolver.teamSharedIntelFor(true, false));
        MinimapViewerContext ffa = CSMinimapIdentityResolver.resolve(
                A, "1", true, false, false, false
        );
        assertEquals(ViewerRole.ACTIVE_PLAYER, ffa.role());
        assertFalse(ffa.teamSharedIntelEnabled());
    }
}
package com.phasetranscrystal.blockoffensive.minimap;

import com.phasetranscrystal.fpsmatch.core.minimap.marker.MarkerCandidate;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.MinimapViewerContext;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.ViewerRole;
import com.phasetranscrystal.fpsmatch.core.minimap.model.NamespacedId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CSGameObjectiveTrackerTest {
    private static final UUID CARRIER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DEFUSER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ENTITY = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID DROP_ENTITY = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Test
    void startsNone() {
        CSGameObjectiveTracker tracker = new CSGameObjectiveTracker();
        assertEquals(C4ObjectivePhase.NONE, tracker.snapshot().phase());
        assertFalse(tracker.snapshot().isActiveObjective());
    }

    @Test
    void assignedCarrierThenForceDropThenPickup() {
        CSGameObjectiveTracker tracker = new CSGameObjectiveTracker();
        tracker.assignCarrier(CARRIER, 1, 10, 64, 20, 90f, Optional.empty());
        assertEquals(C4ObjectivePhase.CARRIED, tracker.snapshot().phase());
        assertEquals(Optional.of(CARRIER), tracker.snapshot().carrierId());

        tracker.forceDrop(42, DROP_ENTITY, 5, 11, 64, 21, 0f, Optional.empty());
        assertEquals(C4ObjectivePhase.DROPPED, tracker.snapshot().phase());
        assertEquals(42, tracker.snapshot().entityId().getAsInt());
        assertEquals(Optional.of(DROP_ENTITY), tracker.snapshot().entityUuid());
        assertTrue(tracker.snapshot().carrierId().isEmpty());
        assertFalse(tracker.snapshot().droppedKnownToCt());

        tracker.successfulPickup(CARRIER, 10, 12, 64, 22, 45f, Optional.empty());
        assertEquals(C4ObjectivePhase.CARRIED, tracker.snapshot().phase());
        assertEquals(Optional.of(CARRIER), tracker.snapshot().carrierId());
        assertTrue(tracker.snapshot().entityId().isEmpty());
    }

    @Test
    void manualDropPreservesStableEntityIdentity() {
        CSGameObjectiveTracker tracker = new CSGameObjectiveTracker();
        tracker.assignCarrier(CARRIER, 1, 0, 0, 0, 0f, Optional.empty());
        tracker.manualDrop(7, ENTITY, 2, 1, 2, 3, 10f, Optional.of("floor_1"));
        assertEquals(7, tracker.stableEntityId().getAsInt());
        assertEquals(Optional.of(ENTITY), tracker.stableEntityUuid());
        tracker.noteDroppedPose(7, 3, 4, 5, 6, 20f, Optional.of("floor_1"));
        assertEquals(7, tracker.snapshot().entityId().getAsInt());
        assertEquals(4, tracker.snapshot().x(), 1e-9);
    }

    @Test
    void plantedDefusingCancelDefusedExplodedRemovedRoundReset() {
        CSGameObjectiveTracker tracker = new CSGameObjectiveTracker();
        tracker.assignCarrier(CARRIER, 1, 0, 0, 0, 0f, Optional.empty());
        tracker.planted(100, ENTITY, 20, 50, 64, 50, 0f, Optional.empty(), Optional.of("site_1"));
        assertEquals(C4ObjectivePhase.PLANTED, tracker.snapshot().phase());
        assertEquals(Optional.of("site_1"), tracker.snapshot().siteId());
        assertEquals(100, tracker.stableEntityId().getAsInt());

        tracker.startDefusing(DEFUSER, 25, 0.1f);
        assertEquals(C4ObjectivePhase.DEFUSING, tracker.snapshot().phase());
        assertEquals(Optional.of(DEFUSER), tracker.snapshot().defuserId());
        assertEquals(0.1f, tracker.snapshot().defuseProgress().orElse(-1f), 1e-6);

        tracker.updateDefuseProgress(DEFUSER, 26, 0.5f);
        assertEquals(0.5f, tracker.snapshot().defuseProgress().orElse(-1f), 1e-6);

        tracker.cancelDefuse(27);
        assertEquals(C4ObjectivePhase.PLANTED, tracker.snapshot().phase());
        assertTrue(tracker.snapshot().defuserId().isEmpty());

        tracker.startDefusing(DEFUSER, 28, 0.9f);
        tracker.defused(30);
        assertEquals(C4ObjectivePhase.DEFUSED, tracker.snapshot().phase());

        tracker.roundReset();
        assertEquals(C4ObjectivePhase.NONE, tracker.snapshot().phase());

        tracker.planted(101, ENTITY, 40, 1, 1, 1, 0f, Optional.empty(), Optional.of("site_2"));
        tracker.exploded(50);
        assertEquals(C4ObjectivePhase.EXPLODED, tracker.snapshot().phase());
        tracker.removed(51);
        assertEquals(C4ObjectivePhase.REMOVED, tracker.snapshot().phase());
        assertTrue(tracker.snapshot().entityId().isEmpty());
    }

    @Test
    void carrierDisconnectOrDeathForcesDropWithEntityIdentity() {
        CSGameObjectiveTracker tracker = new CSGameObjectiveTracker();
        tracker.assignCarrier(CARRIER, 1, 8, 9, 10, 30f, Optional.empty());
        tracker.carrierDisconnectedOrDied(55, DROP_ENTITY, 2, 8, 9, 10, 30f, Optional.empty());
        assertEquals(C4ObjectivePhase.DROPPED, tracker.snapshot().phase());
        assertEquals(55, tracker.snapshot().entityId().getAsInt());
        assertEquals(Optional.of(DROP_ENTITY), tracker.snapshot().entityUuid());
    }

    @Test
    void droppedCtDiscoveryAndLastKnownExpiry() {
        CSGameObjectiveTracker tracker = new CSGameObjectiveTracker(60);
        tracker.forceDrop(3, DROP_ENTITY, 10, 1, 2, 3, 0f, Optional.empty());
        assertFalse(tracker.snapshot().droppedKnownToCt());

        tracker.noteDroppedDiscoveredByCt(15, 1, 2, 3, 0f, Optional.empty());
        assertTrue(tracker.snapshot().droppedKnownToCt());
        assertEquals(Optional.of(75L), tracker.snapshot().droppedCtLastKnownUntil());

        tracker.tick(75);
        assertTrue(tracker.snapshot().droppedKnownToCt());
        tracker.tick(76);
        assertFalse(tracker.snapshot().droppedKnownToCt());
        assertTrue(tracker.snapshot().droppedCtLastKnownUntil().isEmpty());
    }

    @Test
    void visibilityMatrixCarriedDroppedPlantedDefusing() {
        AtomicReference<C4ObjectiveSnapshot> snap = new AtomicReference<>(C4ObjectiveSnapshot.none());
        AtomicLong tick = new AtomicLong(100);
        AtomicReference<Optional<Boolean>> ctIntel = new AtomicReference<>(Optional.of(false));
        CSGameObjectiveMarkerProvider provider = new CSGameObjectiveMarkerProvider(snap::get, tick::get, ctIntel::get);

        MinimapViewerContext t = viewer(ViewerRole.ACTIVE_PLAYER, "t", true);
        MinimapViewerContext ct = viewer(ViewerRole.ACTIVE_PLAYER, "ct", true);
        MinimapViewerContext deadT = viewer(ViewerRole.DEAD_TEAM_MEMBER, "t", true);
        MinimapViewerContext deadCt = viewer(ViewerRole.DEAD_TEAM_MEMBER, "ct", true);
        MinimapViewerContext observer = new MinimapViewerContext(ViewerRole.SPECTATOR_TEAM, "observer", Optional.empty(), false, true);
        MinimapViewerContext observerLimited = new MinimapViewerContext(ViewerRole.SPECTATOR_TEAM, "observer", Optional.empty(), false, false);

        // CARRIED
        CSGameObjectiveTracker tracker = new CSGameObjectiveTracker();
        tracker.assignCarrier(CARRIER, 100, 1, 1, 1, 0f, Optional.empty());
        snap.set(tracker.snapshot());
        assertFalse(provider.collect(t).isEmpty());
        assertFalse(provider.collect(deadT).isEmpty());
        assertTrue(provider.collect(ct).isEmpty());
        assertTrue(provider.collect(deadCt).isEmpty());
        assertFalse(provider.collect(observer).isEmpty());
        assertTrue(provider.collect(observerLimited).isEmpty());

        ctIntel.set(Optional.of(true));
        assertFalse(provider.collect(ct).isEmpty());
        assertFalse(provider.collect(deadCt).isEmpty());

        // DROPPED unknown to CT
        tracker.forceDrop(9, DROP_ENTITY, 110, 2, 2, 2, 0f, Optional.empty());
        snap.set(tracker.snapshot());
        ctIntel.set(Optional.of(false));
        assertFalse(provider.collect(t).isEmpty());
        assertTrue(provider.collect(ct).isEmpty());
        assertFalse(provider.collect(observer).isEmpty());

        tracker.noteDroppedDiscoveredByCt(111, 2, 2, 2, 0f, Optional.empty());
        snap.set(tracker.snapshot());
        assertFalse(provider.collect(ct).isEmpty());
        assertFalse(provider.collect(deadCt).isEmpty());

        // PLANTED always
        tracker.planted(200, ENTITY, 120, 5, 5, 5, 0f, Optional.empty(), Optional.of("site_1"));
        snap.set(tracker.snapshot());
        assertFalse(provider.collect(t).isEmpty());
        assertFalse(provider.collect(ct).isEmpty());
        assertFalse(provider.collect(observer).isEmpty());
        assertFalse(provider.collect(observerLimited).isEmpty()); // planted is public even for limited observer path via switch
        // note: limited observer returns true for PLANTED in provider

        // DEFUSING: T sees planted style without progress style; CT sees defusing
        tracker.startDefusing(DEFUSER, 130, 0.25f);
        snap.set(tracker.snapshot());
        List<MarkerCandidate> tMarkers = provider.collect(t);
        List<MarkerCandidate> ctMarkers = provider.collect(ct);
        assertFalse(tMarkers.isEmpty());
        assertEquals(CSGameObjectiveMarkerProvider.STYLE_PLANTED, tMarkers.get(0).styleId());
        assertEquals(CSGameObjectiveMarkerProvider.STYLE_DEFUSING, ctMarkers.get(0).styleId());

        // DEFUSED removes
        tracker.defused(140);
        snap.set(tracker.snapshot());
        assertTrue(provider.collect(t).isEmpty());
        assertTrue(provider.collect(ct).isEmpty());
        assertTrue(provider.collect(observer).isEmpty());
    }

    @Test
    void dropC4SourceRetainsItemEntity() throws Exception {
        Path map = Path.of("src/main/java/com/phasetranscrystal/blockoffensive/map/CSMap.java");
        String src = Files.readString(map, StandardCharsets.UTF_8);
        assertTrue(src.contains("dropC4"), "dropC4 must exist");
        // signature must return ItemEntity (or Optional) for identity retention
        assertTrue(
                src.contains("ItemEntity dropC4") || src.contains("Optional<ItemEntity> dropC4") || src.contains("@Nullable ItemEntity dropC4"),
                "dropC4 must retain/return ItemEntity identity"
        );
        // must not only call player.drop without capturing return
        int idx = src.indexOf("dropC4");
        assertTrue(idx >= 0);
        String method = src.substring(idx, Math.min(src.length(), idx + 800));
        assertFalse(method.contains("player.drop(new ItemStack(BOItemRegister.C4.get(), 1), false, false);")
                        && !method.contains("ItemEntity"),
                "forced drop must capture ItemEntity");
    }

    private static MinimapViewerContext viewer(ViewerRole role, String team, boolean shared) {
        return new MinimapViewerContext(role, team, Optional.empty(), shared, false);
    }
}
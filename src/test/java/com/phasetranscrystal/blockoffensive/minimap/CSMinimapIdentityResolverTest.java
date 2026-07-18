package com.phasetranscrystal.blockoffensive.minimap;

import com.phasetranscrystal.fpsmatch.core.minimap.marker.MinimapViewerContext;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.PlayerPoseSnapshot;
import com.phasetranscrystal.fpsmatch.core.minimap.marker.ViewerRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSMinimapIdentityResolverTest {
    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void livingPlayerIsActiveWithSelfMarker() {
        MinimapViewerContext ctx = CSMinimapIdentityResolver.resolve(
                PLAYER, "ct", true, false, true, false
        );
        assertEquals(ViewerRole.ACTIVE_PLAYER, ctx.role());
        assertEquals("ct", ctx.teamId());
        assertEquals(PlayerPoseSnapshot.markerIdFor(PLAYER), ctx.selfMarkerId().orElseThrow());
        assertTrue(ctx.teamSharedIntelEnabled());
    }

    @Test
    void deadPlayerKeepsOriginalTeamAsDeadMember() {
        MinimapViewerContext ctx = CSMinimapIdentityResolver.resolve(
                PLAYER, "t", false, false, true, false
        );
        assertEquals(ViewerRole.DEAD_TEAM_MEMBER, ctx.role());
        assertEquals("t", ctx.teamId());
    }

    @Test
    void trueObserverUsesSpectatorRoleWithoutInheritingCameraTeam() {
        // camera target team would be "ct"; identity still spectator
        MinimapViewerContext ctx = CSMinimapIdentityResolver.resolve(
                PLAYER, "spectator", false, true, true, true
        );
        assertEquals(ViewerRole.SPECTATOR_TEAM, ctx.role());
        assertEquals("spectator", ctx.teamId());
        assertTrue(ctx.observerOmniscient());
    }

    @Test
    void ffaHasNoSharedIntelWhileTdmAndCsDo() {
        assertFalse(CSMinimapIdentityResolver.teamSharedIntelFor(true, false));
        assertTrue(CSMinimapIdentityResolver.teamSharedIntelFor(true, true));
        assertTrue(CSMinimapIdentityResolver.teamSharedIntelFor(false, false));
    }
}
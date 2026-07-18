package com.phasetranscrystal.blockoffensive.minimap;

import com.phasetranscrystal.fpsmatch.core.minimap.hud.HudSafeAreaRegistry;
import com.phasetranscrystal.fpsmatch.core.minimap.hud.HudSafeAreaResolution;
import com.phasetranscrystal.fpsmatch.core.minimap.hud.ScreenRect;
import com.phasetranscrystal.fpsmatch.core.minimap.model.MapKey;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CSHudSafeAreaLayoutsTest {
    private static final MapKey MAP = new MapKey("cs", "dust2");
    private static final int SW = 855;
    private static final int SH = 480;

    @Test
    void voteBombRosterCardMatchDesignConstants() {
        ScreenRect vote = CSHudSafeAreaLayouts.vote(SW);
        assertEquals(168, vote.width());
        assertEquals(60, vote.height());
        assertEquals(34, vote.y());
        assertEquals((SW - 168) / 2, vote.x());

        ScreenRect bomb = CSHudSafeAreaLayouts.bombFuse(SW);
        assertEquals(120, bomb.width());
        assertEquals(26, bomb.height());
        assertEquals(50, bomb.y());

        ScreenRect roster = CSHudSafeAreaLayouts.spectatorRoster(SW, 3);
        assertEquals(108, roster.width());
        assertEquals(SW - 108 - 6, roster.x());
        assertEquals(60, roster.y());
        assertEquals(16 + 3 * 11 + 4, roster.height());

        ScreenRect card = CSHudSafeAreaLayouts.spectatorCard(SW, SH, 0f);
        assertEquals(320, card.width());
        assertEquals(64, card.height());
        assertEquals((SW - 320) / 2, card.x());
        assertEquals(SH - 64 - 10, card.y());

        ScreenRect cardSlide = CSHudSafeAreaLayouts.spectatorCard(SW, SH, 12f);
        assertEquals(SH - 64 - 10 + 12, cardSlide.y());
    }

    @Test
    void scoreboardUsesScaledTopBandWithAvatarOffsets() {
        ScreenRect rect = CSHudSafeAreaLayouts.scoreboard(SW, SH);
        assertEquals(2, rect.y());
        assertEquals(35, rect.height());
        // center +/- (time 20 + gap 2 + box 24 + offset 26) = 72 each side => width 144
        assertEquals(SW / 2 - 72, rect.x());
        assertEquals(144, rect.width());
    }

    @Test
    void killFeedCornersAndDynamicRows() {
        ScreenRect tl = CSHudSafeAreaLayouts.killFeed(SW, SH, 1, 2, 100);
        assertEquals(10, tl.x());
        assertEquals(10, tl.y());
        assertEquals(100, tl.width());
        assertEquals(32, tl.height());

        ScreenRect tr = CSHudSafeAreaLayouts.killFeed(SW, SH, 2, 1, 80);
        assertEquals(SW - 10 - 80, tr.x());
        assertEquals(10, tr.y());

        ScreenRect br = CSHudSafeAreaLayouts.killFeed(SW, SH, 4, 3, 90);
        assertEquals(SW - 10 - 90, br.x());
        assertEquals(SH - 50, br.y());
        assertEquals(48, br.height());
    }

    @Test
    void contributorsOnlyWhenVisibleAndPriorityStableIds() {
        AtomicBoolean voteVis = new AtomicBoolean(false);
        AtomicBoolean bombVis = new AtomicBoolean(true);
        AtomicInteger rosterRows = new AtomicInteger(0);
        AtomicInteger killRows = new AtomicInteger(0);
        AtomicBoolean cardVis = new AtomicBoolean(false);
        AtomicBoolean scoreVis = new AtomicBoolean(true);
        AtomicReference<Float> slide = new AtomicReference<>(0f);

        CSHudSafeAreaContributors contrib = new CSHudSafeAreaContributors(
                new CSHudSafeAreaContributors.ScoreboardSource(scoreVis::get, () -> SW, () -> SH),
                new CSHudSafeAreaContributors.SimpleTopSource(voteVis::get, () -> SW),
                new CSHudSafeAreaContributors.SimpleTopSource(bombVis::get, () -> SW),
                new CSHudSafeAreaContributors.RosterSource(() -> rosterRows.get() > 0, () -> SW, rosterRows::get),
                new CSHudSafeAreaContributors.KillFeedSource(
                        () -> killRows.get() > 0, () -> SW, () -> SH, () -> 2, killRows::get, () -> 100
                ),
                new CSHudSafeAreaContributors.SpectatorCardSource(cardVis::get, () -> SW, () -> SH, slide::get)
        );

        HudSafeAreaRegistry registry = new HudSafeAreaRegistry();
        registry.beginFrame(MAP, SW, SH);
        contrib.contributeAll(registry);
        HudSafeAreaResolution res = registry.resolve();
        assertTrue(res.placements().containsKey(CSHudSafeAreaLayouts.ID_SCOREBOARD));
        assertTrue(res.placements().containsKey(CSHudSafeAreaLayouts.ID_BOMB_FUSE));
        assertFalse(res.placements().containsKey(CSHudSafeAreaLayouts.ID_VOTE));
        assertFalse(res.placements().containsKey(CSHudSafeAreaLayouts.ID_SPECTATOR_ROSTER));
        assertFalse(res.placements().containsKey(CSHudSafeAreaLayouts.ID_KILL_FEED));
        assertFalse(res.placements().containsKey(CSHudSafeAreaLayouts.ID_SPECTATOR_CARD));
        assertEquals(100, CSHudSafeAreaLayouts.PRIORITY);

        // appear same frame
        voteVis.set(true);
        rosterRows.set(2);
        killRows.set(3);
        cardVis.set(true);
        slide.set(6f);
        registry.beginFrame(MAP, SW, SH);
        contrib.contributeAll(registry);
        res = registry.resolve();
        assertTrue(res.placements().containsKey(CSHudSafeAreaLayouts.ID_VOTE));
        assertEquals(CSHudSafeAreaLayouts.vote(SW), res.placements().get(CSHudSafeAreaLayouts.ID_VOTE).rect().orElseThrow());
        assertEquals(CSHudSafeAreaLayouts.spectatorRoster(SW, 2), res.placements().get(CSHudSafeAreaLayouts.ID_SPECTATOR_ROSTER).rect().orElseThrow());
        assertEquals(CSHudSafeAreaLayouts.killFeed(SW, SH, 2, 3, 100), res.placements().get(CSHudSafeAreaLayouts.ID_KILL_FEED).rect().orElseThrow());
        assertEquals(CSHudSafeAreaLayouts.spectatorCard(SW, SH, 6f), res.placements().get(CSHudSafeAreaLayouts.ID_SPECTATOR_CARD).rect().orElseThrow());

        // disappear same frame
        voteVis.set(false);
        rosterRows.set(0);
        killRows.set(0);
        cardVis.set(false);
        bombVis.set(false);
        scoreVis.set(false);
        registry.beginFrame(MAP, SW, SH);
        contrib.contributeAll(registry);
        res = registry.resolve();
        assertTrue(res.placements().isEmpty());
    }
}
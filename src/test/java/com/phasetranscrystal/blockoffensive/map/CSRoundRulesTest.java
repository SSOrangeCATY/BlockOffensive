package com.phasetranscrystal.blockoffensive.map;

import com.phasetranscrystal.fpsmatch.core.map.BlastBombState;
import com.phasetranscrystal.fpsmatch.core.match.RoundLifecycle;
import com.phasetranscrystal.fpsmatch.core.match.RoundResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CSRoundRulesTest {
    private static final UUID UUID_1 = UUID.randomUUID();
    private static final String T = "cs_de_T";
    private static final String CT = "cs_de_CT";

    static class FakeContext implements CSRoundContext {
        BlastBombState state = BlastBombState.NONE;
        Map<String, List<UUID>> living = new HashMap<>();
        boolean enoughPlayers = true;
        boolean debug = false;
        Set<String> bombPlacers = new HashSet<>(Collections.singletonList(T));

        @Override public BlastBombState blastState() { return state; }
        @Override public Map<String, List<UUID>> livingTeams() { return living; }
        @Override public boolean hasEnoughPlayers() { return enoughPlayers; }
        @Override public boolean canPlaceBombs(String teamName) { return bombPlacers.contains(teamName); }
        @Override public String tTeamName() { return T; }
        @Override public String ctTeamName() { return CT; }
        @Override public boolean isDebug() { return debug; }
    }

    private static RoundLifecycle<String, CSRoundResultReason> emptyLifecycle() {
        return RoundLifecycle.<String, CSRoundResultReason>builder().build();
    }

    private static RoundLifecycle<String, CSRoundResultReason> lifecycleWithRoundTicks(int ticks) {
        return RoundLifecycle.<String, CSRoundResultReason>builder()
                .waitingTicks(0)
                .roundTicks(ticks)
                .build();
    }

    @Test
    void bombExploded_returnsT() {
        FakeContext ctx = new FakeContext();
        ctx.state = BlastBombState.EXPLODED;

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSBombExplodedRule().evaluate(emptyLifecycle(), ctx);

        assertTrue(result.isPresent());
        assertEquals(T, result.get().winner());
        assertEquals(CSRoundResultReason.DETONATE_BOMB, result.get().reason());
    }

    @Test
    void bombDefused_returnsCT() {
        FakeContext ctx = new FakeContext();
        ctx.state = BlastBombState.DEFUSED;

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSBombDefusedRule().evaluate(emptyLifecycle(), ctx);

        assertTrue(result.isPresent());
        assertEquals(CT, result.get().winner());
        assertEquals(CSRoundResultReason.DEFUSE_BOMB, result.get().reason());
    }

    @Test
    void elimination_noneStateTAlive_returnsT() {
        FakeContext ctx = new FakeContext();
        ctx.living.put(T, List.of(UUID_1));

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSEliminationRule().evaluate(emptyLifecycle(), ctx);

        assertTrue(result.isPresent());
        assertEquals(T, result.get().winner());
        assertEquals(CSRoundResultReason.ACED, result.get().reason());
    }

    @Test
    void elimination_noneStateCTAlive_returnsCT() {
        FakeContext ctx = new FakeContext();
        ctx.living.put(CT, List.of(UUID_1));

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSEliminationRule().evaluate(emptyLifecycle(), ctx);

        assertTrue(result.isPresent());
        assertEquals(CT, result.get().winner());
        assertEquals(CSRoundResultReason.ACED, result.get().reason());
    }

    @Test
    void elimination_noneStateBothDead_returnsCT() {
        FakeContext ctx = new FakeContext();

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSEliminationRule().evaluate(emptyLifecycle(), ctx);

        assertTrue(result.isPresent());
        assertEquals(CT, result.get().winner());
        assertEquals(CSRoundResultReason.ACED, result.get().reason());
    }

    @Test
    void elimination_tickingStateCTOnly_noResult() {
        FakeContext ctx = new FakeContext();
        ctx.state = BlastBombState.TICKING;
        ctx.living.put(CT, List.of(UUID_1));

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSEliminationRule().evaluate(emptyLifecycle(), ctx);

        assertTrue(result.isEmpty());
    }

    @Test
    void elimination_tickingStateTOnly_returnsT() {
        FakeContext ctx = new FakeContext();
        ctx.state = BlastBombState.TICKING;
        ctx.living.put(T, List.of(UUID_1));

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSEliminationRule().evaluate(emptyLifecycle(), ctx);

        assertTrue(result.isPresent());
        assertEquals(T, result.get().winner());
        assertEquals(CSRoundResultReason.ACED, result.get().reason());
    }

    @Test
    void elimination_tickingStateBothDead_returnsT() {
        FakeContext ctx = new FakeContext();
        ctx.state = BlastBombState.TICKING;

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSEliminationRule().evaluate(emptyLifecycle(), ctx);

        assertTrue(result.isPresent());
        assertEquals(T, result.get().winner());
        assertEquals(CSRoundResultReason.ACED, result.get().reason());
    }

    @Test
    void timeout_noneStateAndRoundElapsed_returnsCT() {
        FakeContext ctx = new FakeContext();
        RoundLifecycle<String, CSRoundResultReason> lifecycle = lifecycleWithRoundTicks(2);
        lifecycle.tick();
        lifecycle.tick();
        lifecycle.tick();

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSRoundTimeoutRule().evaluate(lifecycle, ctx);

        assertTrue(result.isPresent());
        assertEquals(CT, result.get().winner());
        assertEquals(CSRoundResultReason.TIME_OUT, result.get().reason());
    }

    @Test
    void timeout_tickingState_noResult() {
        FakeContext ctx = new FakeContext();
        ctx.state = BlastBombState.TICKING;
        RoundLifecycle<String, CSRoundResultReason> lifecycle = lifecycleWithRoundTicks(0);
        lifecycle.tick();

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSRoundTimeoutRule().evaluate(lifecycle, ctx);

        assertTrue(result.isEmpty());
    }

    @Test
    void elimination_notEnoughPlayers_noResult() {
        FakeContext ctx = new FakeContext();
        ctx.enoughPlayers = false;
        ctx.living.put(T, List.of(UUID_1));

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSEliminationRule().evaluate(emptyLifecycle(), ctx);

        assertTrue(result.isEmpty());
    }

    @Test
    void elimination_debug_noResult() {
        FakeContext ctx = new FakeContext();
        ctx.debug = true;
        ctx.living.put(T, List.of(UUID_1));

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSEliminationRule().evaluate(emptyLifecycle(), ctx);

        assertTrue(result.isEmpty());
    }
}

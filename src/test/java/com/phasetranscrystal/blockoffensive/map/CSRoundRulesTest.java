package com.phasetranscrystal.blockoffensive.map;

import com.phasetranscrystal.fpsmatch.core.map.BlastBombState;
import com.phasetranscrystal.fpsmatch.core.match.RoundLifecycle;
import com.phasetranscrystal.fpsmatch.core.match.RoundPhase;
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
    void timeout_usesConfiguredLimitWhenLifecycleTimeoutIsDisabled() {
        FakeContext ctx = new FakeContext();
        RoundLifecycle<String, CSRoundResultReason> lifecycle = lifecycleWithRoundTicks(Integer.MAX_VALUE);
        lifecycle.tick();
        lifecycle.tick();
        lifecycle.tick();

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSRoundTimeoutRule(2).evaluate(lifecycle, ctx);

        assertTrue(result.isPresent());
        assertEquals(CT, result.get().winner());
        assertEquals(CSRoundResultReason.TIME_OUT, result.get().reason());
    }

    @Test
    void timeout_configuredLimitStillWaitsForTickingBomb() {
        FakeContext ctx = new FakeContext();
        ctx.state = BlastBombState.TICKING;
        RoundLifecycle<String, CSRoundResultReason> lifecycle = lifecycleWithRoundTicks(Integer.MAX_VALUE);
        lifecycle.tick();

        Optional<RoundResult<String, CSRoundResultReason>> result = new CSRoundTimeoutRule(0).evaluate(lifecycle, ctx);

        assertTrue(result.isEmpty());
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
    void lifecycleTimeout_tickingStateKeepsRoundActive() {
        FakeContext ctx = new FakeContext();
        ctx.state = BlastBombState.TICKING;
        List<RoundResult<String, CSRoundResultReason>> results = new ArrayList<>();
        RoundLifecycle<String, CSRoundResultReason> lifecycle = RoundLifecycle.<String, CSRoundResultReason>builder()
                .waitingTicks(0)
                .roundTicks(1)
                .roundEndTicks(1)
                .addRule(new CSRoundTimeoutRule())
                .timeoutResult(() -> ctx.blastState() == BlastBombState.NONE
                        ? new RoundResult<>(ctx.ctTeamName(), CSRoundResultReason.TIME_OUT)
                        : null)
                .onRoundEnd(results::add)
                .build();

        lifecycle.tick(ctx);
        lifecycle.tick(ctx);
        lifecycle.tick(ctx);

        assertEquals(RoundPhase.ACTIVE_ROUND, lifecycle.phase());
        assertTrue(results.isEmpty());
        assertEquals(3, lifecycle.roundElapsedTicks());
    }

    @Test
    void lifecycleTimeout_noneStateEndsRoundForCT() {
        FakeContext ctx = new FakeContext();
        List<RoundResult<String, CSRoundResultReason>> results = new ArrayList<>();
        RoundLifecycle<String, CSRoundResultReason> lifecycle = RoundLifecycle.<String, CSRoundResultReason>builder()
                .waitingTicks(0)
                .roundTicks(1)
                .roundEndTicks(1)
                .addRule(new CSRoundTimeoutRule())
                .timeoutResult(() -> ctx.blastState() == BlastBombState.NONE
                        ? new RoundResult<>(ctx.ctTeamName(), CSRoundResultReason.TIME_OUT)
                        : null)
                .onRoundEnd(results::add)
                .build();

        lifecycle.tick(ctx);
        lifecycle.tick(ctx);

        assertEquals(RoundPhase.ROUND_END_WAITING, lifecycle.phase());
        assertEquals(List.of(new RoundResult<>(CT, CSRoundResultReason.TIME_OUT)), results);
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

    @Test
    void nextRoundLossMinimumMoneyIncreasesWithLossStreak() {
        assertEquals(1400, CSEconomyRules.calculateNextRoundMinMoney(1));
        assertEquals(1900, CSEconomyRules.calculateNextRoundMinMoney(2));
        assertEquals(2400, CSEconomyRules.calculateNextRoundMinMoney(3));
        assertEquals(2900, CSEconomyRules.calculateNextRoundMinMoney(4));
    }

    @Test
    void nextRoundLossMinimumMoneyTreatsMissingCompensationAsBase() {
        assertEquals(1400, CSEconomyRules.calculateNextRoundMinMoney(null));
    }
}

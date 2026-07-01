package com.phasetranscrystal.blockoffensive.map;

import com.phasetranscrystal.fpsmatch.core.map.BlastBombState;
import com.phasetranscrystal.fpsmatch.core.match.RoundContext;
import com.phasetranscrystal.fpsmatch.core.match.RoundLifecycle;
import com.phasetranscrystal.fpsmatch.core.match.RoundResult;
import com.phasetranscrystal.fpsmatch.core.match.RoundRuleWithContext;

import java.util.Optional;

class CSRoundTimeoutRule implements RoundRuleWithContext<String, CSRoundResultReason> {
    private final Integer roundTicks;

    CSRoundTimeoutRule() {
        this.roundTicks = null;
    }

    CSRoundTimeoutRule(int roundTicks) {
        this.roundTicks = Math.max(0, roundTicks);
    }

    @Override
    public Optional<RoundResult<String, CSRoundResultReason>> evaluate(RoundLifecycle<String, CSRoundResultReason> lifecycle, RoundContext context) {
        CSRoundContext ctx = (CSRoundContext) context;
        int limit = roundTicks == null ? lifecycle.roundTicks() : roundTicks;
        if (ctx.blastState() != BlastBombState.NONE || lifecycle.roundElapsedTicks() < limit) {
            return Optional.empty();
        }
        return Optional.of(new RoundResult<>(ctx.ctTeamName(), CSRoundResultReason.TIME_OUT));
    }
}

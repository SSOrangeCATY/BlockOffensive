package com.phasetranscrystal.blockoffensive.mvp;

import java.util.Comparator;
import java.util.List;

public final class CSMvpScorer {
    private CSMvpScorer() {
    }

    public static CSMvpResult selectRoundMvp(List<CSMvpContribution> contributions) {
        return contributions.stream()
                .max(Comparator.comparingInt(CSMvpScorer::score)
                        .thenComparingInt(CSMvpScorer::priority)
                        .thenComparingDouble(CSMvpContribution::damage)
                        .thenComparingInt(contribution -> contribution.incendiaryDamage() > 0 ? 1 : 0))
                .map(contribution -> new CSMvpResult(
                        contribution.uuid(),
                        reasonKey(contribution),
                        infoKey(contribution),
                        score(contribution)
                ))
                .orElse(null);
    }

    private static int score(CSMvpContribution contribution) {
        int objectiveScore = 0;
        if (contribution.defusedBomb() || contribution.plantedBomb()) {
            objectiveScore = 300;
        }
        return objectiveScore + contribution.kills() * 3 + contribution.assists()
                + Math.round(contribution.damage() / 50.0F)
                + Math.round(contribution.incendiaryDamage() / 25.0F)
                + Math.round(contribution.explosiveDamage() / 45.0F);
    }

    private static int priority(CSMvpContribution contribution) {
        if (contribution.defusedBomb()) {
            return 400;
        }
        if (contribution.plantedBomb()) {
            return 300;
        }
        if (contribution.incendiaryDamage() > 0) {
            return 200;
        }
        if (contribution.explosiveDamage() > 0) {
            return 150;
        }
        if (contribution.damage() > 0) {
            return 100;
        }
        return 0;
    }

    private static String reasonKey(CSMvpContribution contribution) {
        if (contribution.defusedBomb()) {
            return "blockoffensive.mvp.defuse";
        }
        if (contribution.plantedBomb()) {
            return "blockoffensive.mvp.detonate";
        }
        if (contribution.incendiaryDamage() > 0) {
            return "blockoffensive.mvp.incendiary_damage";
        }
        if (contribution.explosiveDamage() > 0) {
            return "blockoffensive.mvp.explosive_damage";
        }
        if (contribution.damage() > 0) {
            return "blockoffensive.mvp.high_damage";
        }
        return "blockoffensive.mvp.combat";
    }

    private static String infoKey(CSMvpContribution contribution) {
        if (contribution.defusedBomb()) {
            return "blockoffensive.mvp.info.defuse";
        }
        if (contribution.plantedBomb()) {
            return "blockoffensive.mvp.info.detonate";
        }
        if (contribution.incendiaryDamage() > 0) {
            return "blockoffensive.mvp.info.incendiary_damage";
        }
        if (contribution.explosiveDamage() > 0) {
            return "blockoffensive.mvp.info.explosive_damage";
        }
        if (contribution.damage() > 0) {
            return "blockoffensive.mvp.info.high_damage";
        }
        return "blockoffensive.mvp.info.combat";
    }
}

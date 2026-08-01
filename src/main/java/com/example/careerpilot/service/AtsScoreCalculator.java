package com.example.careerpilot.service;

import org.springframework.stereotype.Component;

@Component
public class AtsScoreCalculator {

    private static final double KEYWORD_WEIGHT = 0.35;
    private static final double IMPACT_WEIGHT = 0.25;
    private static final double READABILITY_WEIGHT = 0.20;
    private static final double GRAMMAR_WEIGHT = 0.10;
    private static final double STRUCTURE_WEIGHT = 0.10;

    public int calculate(
            int keywordMatch,
            int impact,
            int readability,
            int grammar,
            int structure
    ) {

        int safeKeywordMatch = clamp(keywordMatch);
        int safeImpact = clamp(impact);
        int safeReadability = clamp(readability);
        int safeGrammar = clamp(grammar);
        int safeStructure = clamp(structure);

        double weightedScore =
                safeKeywordMatch * KEYWORD_WEIGHT
                        + safeImpact * IMPACT_WEIGHT
                        + safeReadability * READABILITY_WEIGHT
                        + safeGrammar * GRAMMAR_WEIGHT
                        + safeStructure * STRUCTURE_WEIGHT;

        return (int) Math.round(weightedScore);
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
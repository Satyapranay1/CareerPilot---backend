package com.example.careerpilot.service;

import com.example.careerpilot.dto.KeywordAnalysisResult;
import com.example.careerpilot.dto.RoleExpectation;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class KeywordMatcher {

    /*
     * Canonical skill -> aliases.
     *
     * Keep this list limited to genuine terminology equivalences.
     * Company/role requirements themselves must come from RAG.
     */
    private static final Map<String, Set<String>> ALIASES =
            createAliases();

    public KeywordAnalysisResult analyze(
            String resumeText,
            List<RoleExpectation> expectations
    ) {

        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException(
                    "Resume text cannot be empty."
            );
        }

        if (expectations == null || expectations.isEmpty()) {
            throw new IllegalArgumentException(
                    "Role expectations cannot be empty."
            );
        }

        String normalizedResume = normalize(resumeText);

        List<String> matchedKeywords = new ArrayList<>();
        List<String> missingKeywords = new ArrayList<>();
        List<String> weakKeywords = new ArrayList<>();

        /*
         * Prevent the same logical requirement from contributing
         * more than once if duplicate/alias expectations are
         * returned by the knowledge base.
         */
        Set<String> processedRequirements = new HashSet<>();

        int totalWeight = 0;
        double earnedWeight = 0;

        for (RoleExpectation expectation : expectations) {

            if (expectation == null
                    || expectation.getKeyword() == null
                    || expectation.getKeyword().isBlank()) {
                continue;
            }

            String displayKeyword =
                    expectation.getKeyword().trim();

            String canonicalKeyword =
                    canonicalize(displayKeyword);

            if (!processedRequirements.add(canonicalKeyword)) {
                continue;
            }

            int weight = clampImportance(
                    expectation.getImportance()
            );

            totalWeight += weight;

            MatchStrength matchStrength =
                    determineMatchStrength(
                            normalizedResume,
                            displayKeyword
                    );

            switch (matchStrength) {

                case STRONG -> {
                    matchedKeywords.add(displayKeyword);
                    earnedWeight += weight;
                }

                case WEAK -> {
                    weakKeywords.add(displayKeyword);

                    /*
                     * Weak evidence receives partial credit.
                     */
                    earnedWeight += weight * 0.5;
                }

                case NONE ->
                        missingKeywords.add(displayKeyword);
            }
        }

        if (totalWeight == 0) {
            throw new IllegalArgumentException(
                    "No valid role expectations were provided."
            );
        }

        int keywordMatchScore = (int) Math.round(
                (earnedWeight / totalWeight) * 100
        );

        keywordMatchScore =
                Math.max(0, Math.min(100, keywordMatchScore));

        return new KeywordAnalysisResult(
                keywordMatchScore,
                matchedKeywords,
                missingKeywords,
                weakKeywords
        );
    }

    private MatchStrength determineMatchStrength(
            String normalizedResume,
            String keyword
    ) {

        String canonicalKeyword =
                canonicalize(keyword);

        Set<String> terms = new LinkedHashSet<>();

        terms.add(canonicalKeyword);

        Set<String> aliases =
                ALIASES.get(canonicalKeyword);

        if (aliases != null) {
            terms.addAll(aliases);
        }

        /*
         * Strong match:
         * canonical skill or a genuine alias appears explicitly.
         */
        for (String term : terms) {

            String normalizedTerm = normalize(term);

            if (containsPhrase(
                    normalizedResume,
                    normalizedTerm
            )) {
                return MatchStrength.STRONG;
            }
        }

        /*
         * Weak match:
         * a meaningful part of a compound requirement appears,
         * but not enough to claim full coverage.
         *
         * Example:
         * expectation = JPA Hibernate
         * resume only mentions Hibernate-related terminology.
         */
        if (hasPartialCompoundMatch(
                normalizedResume,
                canonicalKeyword
        )) {
            return MatchStrength.WEAK;
        }

        return MatchStrength.NONE;
    }

    private boolean hasPartialCompoundMatch(
            String normalizedResume,
            String canonicalKeyword
    ) {

        String[] tokens =
                canonicalKeyword.split("\\s+");

        /*
         * Don't mark single-word skills as weak merely because
         * a fragment happens to occur somewhere.
         */
        if (tokens.length < 2) {
            return false;
        }

        int meaningfulTokens = 0;
        int matchedTokens = 0;

        for (String token : tokens) {

            if (!isMeaningfulToken(token)) {
                continue;
            }

            meaningfulTokens++;

            if (containsPhrase(
                    normalizedResume,
                    token
            )) {
                matchedTokens++;
            }
        }

        return meaningfulTokens >= 2
                && matchedTokens > 0
                && matchedTokens < meaningfulTokens;
    }

    private boolean isMeaningfulToken(String token) {

        return token != null
                && token.length() >= 3;
    }

    private boolean containsPhrase(
            String normalizedText,
            String normalizedPhrase
    ) {

        if (normalizedPhrase == null
                || normalizedPhrase.isBlank()) {
            return false;
        }

        /*
         * Normalization leaves only letters, numbers and spaces,
         * so word-boundary matching prevents:
         *
         * java -> javascript
         * rest -> forest
         */
        Pattern pattern = Pattern.compile(
                "(?:^|\\s)"
                        + Pattern.quote(normalizedPhrase)
                        + "(?:$|\\s)"
        );

        return pattern.matcher(normalizedText).find();
    }

    private String canonicalize(String keyword) {

        String normalized = normalize(keyword);

        for (Map.Entry<String, Set<String>> entry
                : ALIASES.entrySet()) {

            if (entry.getKey().equals(normalized)) {
                return entry.getKey();
            }

            for (String alias : entry.getValue()) {

                if (normalize(alias).equals(normalized)) {
                    return entry.getKey();
                }
            }
        }

        return normalized;
    }

    private int clampImportance(int importance) {

        if (importance < 1) {
            return 1;
        }

        return Math.min(importance, 5);
    }

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                /*
                 * Must happen before toLowerCase().
                 *
                 * SpringBoot -> Spring Boot
                 */
                .replaceAll(
                        "([a-z])([A-Z])",
                        "$1 $2"
                )
                .toLowerCase(Locale.ROOT)
                .replace("&", " and ")
                .replaceAll(
                        "[^a-z0-9+#.]+",
                        " "
                )
                .replace("springboot", "spring boot")
                .replace("restapis", "rest api")
                .replace("restapi", "rest api")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static Map<String, Set<String>> createAliases() {

        Map<String, Set<String>> aliases =
                new LinkedHashMap<>();

        aliases.put(
                "sql",
                Set.of(
                        "postgresql",
                        "postgres",
                        "mysql",
                        "relational database",
                        "relational databases"
                )
        );

        aliases.put(
                "spring boot",
                Set.of(
                        "springboot"
                )
        );

        aliases.put(
                "rest",
                Set.of(
                        "rest api",
                        "rest apis",
                        "restful api",
                        "restful apis",
                        "restful services",
                        "rest services"
                )
        );

        aliases.put(
                "jpa",
                Set.of(
                        "java persistence api"
                )
        );

        aliases.put(
                "hibernate",
                Set.of(
                        "hibernate orm"
                )
        );

        aliases.put(
                "javascript",
                Set.of(
                        "js"
                )
        );

        aliases.put(
                "typescript",
                Set.of(
                        "ts"
                )
        );

        aliases.put(
                "amazon web services",
                Set.of(
                        "aws"
                )
        );

        aliases.put(
                "google cloud platform",
                Set.of(
                        "gcp"
                )
        );

        aliases.put(
                "continuous integration continuous delivery",
                Set.of(
                        "ci cd",
                        "ci/cd",
                        "continuous integration",
                        "continuous delivery",
                        "continuous deployment"
                )
        );

        aliases.put(
                "data structures and algorithms",
                Set.of(
                        "dsa",
                        "data structures",
                        "algorithms"
                )
        );

        return Collections.unmodifiableMap(aliases);
    }

    private enum MatchStrength {
        STRONG,
        WEAK,
        NONE
    }
}
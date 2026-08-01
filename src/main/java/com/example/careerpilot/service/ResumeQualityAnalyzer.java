package com.example.careerpilot.service;

import com.example.careerpilot.dto.KeywordAnalysisResult;
import com.example.careerpilot.dto.QualityAnalysisResult;
import com.example.careerpilot.dto.ResumeAiAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ResumeQualityAnalyzer {

    private static final int MAX_AI_ITEMS = 8;

    private final ChatClient.Builder chatClientBuilder;

    public QualityAnalysisResult analyze(
            String resumeText,
            KeywordAnalysisResult keywordAnalysis
    ) {

        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException(
                    "Resume text cannot be empty."
            );
        }

        int readabilityScore =
                calculateReadability(resumeText);

        int structureScore =
                calculateStructure(resumeText);

        ResumeAiAnalysis aiAnalysis =
                analyzeWithAi(
                        resumeText,
                        keywordAnalysis
                );

        int impactScore =
                clamp(aiAnalysis.getImpactScore());

        int grammarScore =
                clamp(aiAnalysis.getGrammarScore());

        List<String> strongAreas =
                sanitizeItems(
                        aiAnalysis.getStrongAreas()
                );

        List<String> suggestions =
                sanitizeItems(
                        aiAnalysis.getSuggestions()
                );

        return new QualityAnalysisResult(
                impactScore,
                readabilityScore,
                grammarScore,
                structureScore,
                strongAreas,
                suggestions
        );
    }

    private ResumeAiAnalysis analyzeWithAi(
            String resumeText,
            KeywordAnalysisResult keywordAnalysis
    ) {

        ChatClient chatClient =
                chatClientBuilder.build();

        try {

            ResumeAiAnalysis result = chatClient
                    .prompt()
                    .system("""
                            You are analyzing resume writing quality.

                            Your responsibilities are limited to:

                            1. Impact score from 0 to 100.
                            2. Grammar score from 0 to 100.
                            3. Strong areas already demonstrated by the resume.
                            4. Actionable improvement suggestions.

                            Impact scoring should consider:
                            - action-oriented statements
                            - measurable results
                            - quantified achievements
                            - ownership
                            - technical or business outcomes
                            - specificity of accomplishments

                            Grammar scoring should consider:
                            - obvious grammar problems
                            - spelling problems
                            - awkward wording
                            - incomplete or unclear statements

                            Important rules:

                            - Do not calculate an ATS score.
                            - Do not calculate keyword match.
                            - Do not invent candidate experience.
                            - Do not claim the candidate knows a missing skill.
                            - Suggestions involving missing skills must say to
                              consider learning/gaining experience unless the
                              resume already demonstrates that experience.
                            - Suggestions should be concise and actionable.
                            - Strong areas must be supported by the resume.
                            """)
                    .user(buildPrompt(
                            resumeText,
                            keywordAnalysis
                    ))
                    .call()
                    .entity(ResumeAiAnalysis.class);

            if (result == null) {
                throw new IllegalStateException(
                        "AI returned no analysis."
                );
            }

            validateAiAnalysis(result);

            return result;

        } catch (Exception exception) {

            /*
             * We intentionally fail analysis here rather than
             * persisting a misleading result.
             *
             * ResumeAnalysisService will make sure a failed
             * analysis does not leave an analysis record/file.
             */
            throw new IllegalStateException(
                    "Resume quality analysis could not be completed.",
                    exception
            );
        }
    }

    private String buildPrompt(
            String resumeText,
            KeywordAnalysisResult keywordAnalysis
    ) {

        List<String> matched =
                keywordAnalysis == null
                        ? List.of()
                        : safeList(
                        keywordAnalysis.getMatchedKeywords()
                );

        List<String> weak =
                keywordAnalysis == null
                        ? List.of()
                        : safeList(
                        keywordAnalysis.getWeakKeywords()
                );

        List<String> missing =
                keywordAnalysis == null
                        ? List.of()
                        : safeList(
                        keywordAnalysis.getMissingKeywords()
                );

        return """
                Analyze the following resume.

                Matched role requirements:
                %s

                Weakly demonstrated requirements:
                %s

                Missing role requirements:
                %s

                Resume:
                --- BEGIN RESUME ---
                %s
                --- END RESUME ---

                Return the requested structured analysis only.
                """.formatted(
                matched,
                weak,
                missing,
                resumeText
        );
    }

    private int calculateReadability(String resumeText) {

        int score = 100;

        String[] lines =
                resumeText.split("\\R");

        int nonEmptyLines = 0;
        int longLines = 0;
        int veryLongLines = 0;

        for (String rawLine : lines) {

            String line = rawLine.trim();

            if (line.isBlank()) {
                continue;
            }

            nonEmptyLines++;

            int wordCount =
                    countWords(line);

            if (wordCount > 30) {
                longLines++;
            }

            if (wordCount > 50) {
                veryLongLines++;
            }
        }

        /*
         * Penalize excessive long resume lines/bullets.
         */
        score -= Math.min(longLines * 2, 16);
        score -= Math.min(veryLongLines * 3, 12);

        /*
         * Approximate sentence-level complexity.
         */
        String[] sentences =
                resumeText.split("[.!?]+");

        int validSentences = 0;
        int overlyLongSentences = 0;

        for (String sentence : sentences) {

            int words =
                    countWords(sentence);

            if (words == 0) {
                continue;
            }

            validSentences++;

            if (words > 35) {
                overlyLongSentences++;
            }
        }

        score -= Math.min(
                overlyLongSentences * 2,
                12
        );

        /*
         * Resume density checks.
         */
        int totalWords =
                countWords(resumeText);

        if (totalWords > 1200) {
            score -= 15;
        } else if (totalWords > 900) {
            score -= 8;
        }

        /*
         * Extremely little content is also not very readable/useful
         * as a professional resume.
         */
        if (totalWords < 80) {
            score -= 20;
        }

        /*
         * If most extracted lines are unusually long, the resume
         * likely contains dense paragraph-style content.
         */
        if (nonEmptyLines > 0) {

            double longLineRatio =
                    (double) longLines / nonEmptyLines;

            if (longLineRatio > 0.40) {
                score -= 10;
            }
        }

        return clamp(score);
    }

    private int calculateStructure(String resumeText) {

        String normalized =
                resumeText.toLowerCase(Locale.ROOT);

        boolean hasExperience =
                containsAnyHeading(
                        normalized,
                        "experience",
                        "work experience",
                        "professional experience",
                        "employment"
                );

        boolean hasEducation =
                containsAnyHeading(
                        normalized,
                        "education",
                        "academic background",
                        "academics"
                );

        boolean hasSkills =
                containsAnyHeading(
                        normalized,
                        "skills",
                        "technical skills",
                        "technologies",
                        "tech stack"
                );

        boolean hasProjects =
                containsAnyHeading(
                        normalized,
                        "projects",
                        "project experience",
                        "academic projects",
                        "personal projects"
                );

        boolean hasSummary =
                containsAnyHeading(
                        normalized,
                        "summary",
                        "professional summary",
                        "profile",
                        "objective"
                );

        /*
         * Core sections receive most of the score.
         *
         * Summary is optional and therefore has only a small
         * contribution.
         */
        int score = 0;

        if (hasExperience) {
            score += 30;
        }

        if (hasEducation) {
            score += 25;
        }

        if (hasSkills) {
            score += 25;
        }

        if (hasProjects) {
            score += 15;
        }

        if (hasSummary) {
            score += 5;
        }

        /*
         * Don't penalize experienced candidates too heavily if
         * they omit Projects, or students if Experience is limited.
         *
         * Having three useful professional sections already gives
         * a reasonable structure floor.
         */
        int coreSections = 0;

        if (hasExperience) {
            coreSections++;
        }

        if (hasEducation) {
            coreSections++;
        }

        if (hasSkills) {
            coreSections++;
        }

        if (hasProjects) {
            coreSections++;
        }

        if (coreSections >= 3) {
            score = Math.max(score, 80);
        }

        return clamp(score);
    }

    private boolean containsAnyHeading(
            String normalizedResume,
            String... headings
    ) {

        String[] lines =
                normalizedResume.split("\\R");

        for (String rawLine : lines) {

            String line = rawLine
                    .trim()
                    .replaceAll("[:\\-]+$", "")
                    .trim();

            for (String heading : headings) {

                if (line.equals(heading)) {
                    return true;
                }
            }
        }

        return false;
    }

    private int countWords(String text) {

        if (text == null || text.isBlank()) {
            return 0;
        }

        return text
                .trim()
                .split("\\s+")
                .length;
    }

    private void validateAiAnalysis(
            ResumeAiAnalysis analysis
    ) {

        /*
         * We clamp valid numeric output later, but wildly invalid
         * values indicate malformed/unreliable structured output.
         */
        if (analysis.getImpactScore() < -20
                || analysis.getImpactScore() > 120) {

            throw new IllegalStateException(
                    "AI returned an invalid impact score."
            );
        }

        if (analysis.getGrammarScore() < -20
                || analysis.getGrammarScore() > 120) {

            throw new IllegalStateException(
                    "AI returned an invalid grammar score."
            );
        }

        if (analysis.getStrongAreas() == null) {
            analysis.setStrongAreas(
                    new ArrayList<>()
            );
        }

        if (analysis.getSuggestions() == null) {
            analysis.setSuggestions(
                    new ArrayList<>()
            );
        }
    }

    private List<String> sanitizeItems(
            List<String> items
    ) {

        if (items == null) {
            return new ArrayList<>();
        }

        Set<String> cleaned =
                new LinkedHashSet<>();

        for (String item : items) {

            if (item == null) {
                continue;
            }

            String value = item.trim();

            if (value.isBlank()) {
                continue;
            }

            /*
             * Prevent unexpectedly huge model output from being
             * persisted or sent to the frontend.
             */
            if (value.length() > 300) {
                value = value.substring(0, 300);
            }

            cleaned.add(value);

            if (cleaned.size() >= MAX_AI_ITEMS) {
                break;
            }
        }

        return new ArrayList<>(cleaned);
    }

    private List<String> safeList(
            List<String> values
    ) {

        return values == null
                ? List.of()
                : values;
    }

    private int clamp(int score) {

        return Math.max(
                0,
                Math.min(100, score)
        );
    }
}
package com.example.careerpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {

    private String knowledgeSource;

    private String summary;

    private Double atsScore;

    private Scores scores;

    private List<String> strongAreas;

    private List<String> weakAreas;

    private List<String> missingKeywords;

    private List<String> missingSkills;

    private List<String> improvementSuggestions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Scores {

        private Double keywordMatch;

        private Double impact;

        private Double readability;

        private Double grammar;

        private Double structure;

    }

}
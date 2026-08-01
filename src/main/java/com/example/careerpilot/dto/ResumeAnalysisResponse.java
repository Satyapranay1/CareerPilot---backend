package com.example.careerpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisResponse {

    private Long id;

    private String company;

    private String role;

    private String originalFilename;

    private int atsCompatibilityScore;

    private ResumeScores scores;

    private List<String> matchedKeywords = new ArrayList<>();

    private List<String> missingKeywords = new ArrayList<>();

    private List<String> weakKeywords = new ArrayList<>();

    private List<String> strongAreas = new ArrayList<>();

    private List<String> suggestions = new ArrayList<>();

    private LocalDateTime createdAt;
}
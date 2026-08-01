package com.example.careerpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KeywordAnalysisResult {

    private int keywordMatchScore;

    private List<String> matchedKeywords = new ArrayList<>();

    private List<String> missingKeywords = new ArrayList<>();

    private List<String> weakKeywords = new ArrayList<>();
}
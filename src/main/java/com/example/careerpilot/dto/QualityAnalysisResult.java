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
public class QualityAnalysisResult {

    private int impactScore;

    private int readabilityScore;

    private int grammarScore;

    private int structureScore;

    private List<String> strongAreas = new ArrayList<>();

    private List<String> suggestions = new ArrayList<>();
}
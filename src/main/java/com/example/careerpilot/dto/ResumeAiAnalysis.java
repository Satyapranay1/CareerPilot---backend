package com.example.careerpilot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ResumeAiAnalysis {

    private int impactScore;

    private int grammarScore;

    private List<String> strongAreas = new ArrayList<>();

    private List<String> suggestions = new ArrayList<>();
}
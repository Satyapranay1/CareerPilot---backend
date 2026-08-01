package com.example.careerpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeScores {

    private int keywordMatch;
    private int impact;
    private int readability;
    private int grammar;
    private int structure;
}
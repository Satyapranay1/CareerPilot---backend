package com.example.careerpilot.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {

    private Double atsScore;

    private Double resumeQuality;

    private Double interviewReadiness;

    private Integer solvedQuestions;

    private Double learningHours;

    private Double weeklyProgress;

    private Double skillCoverage;

    private Integer currentStreak;

    private Integer xp;

}
package com.example.careerpilot.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeroSectionDto {

    private String fullName;

    private String targetCompany;

    private String targetRole;

    private String dailyRecommendation;

    private Double interviewReadiness;

    private Integer currentStreak;

    private Integer xp;

}
package com.example.careerpilot.projection;

import java.math.BigDecimal;

public interface DashboardMetricsProjection {

    BigDecimal getAtsScore();

    Long getCodingSolved();

    Double getInterviewReadiness();

    Integer getCurrentStreak();

    Integer getTotalXp();

    Double getLearningHours();

    Double getWeeklyProgress();

    Double getSkillCoverage();

}
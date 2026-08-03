package com.example.careerpilot.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private HeroSectionDto hero;

    private DashboardMetricsDto metrics;

    private List<ReadinessTrendDto> readinessTrend;

    private List<SkillRadarDto> skillRadar;

    private List<WeeklyActivityDto> weeklyActivity;

    private List<TopicDistributionDto> topicDistribution;

    private List<ActivityDto> activities;

    private List<UpcomingTaskDto> upcomingTasks;

}
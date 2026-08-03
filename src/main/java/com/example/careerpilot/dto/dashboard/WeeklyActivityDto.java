package com.example.careerpilot.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyActivityDto {

    private String day;

    private Integer solvedProblems;

    private Integer mockInterviews;

    private Double hoursStudied;

}
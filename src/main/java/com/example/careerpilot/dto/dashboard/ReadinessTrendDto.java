package com.example.careerpilot.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadinessTrendDto {

    private String date;

    private Double readinessScore;

    private Double targetScore;

}
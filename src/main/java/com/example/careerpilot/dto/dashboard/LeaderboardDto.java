package com.example.careerpilot.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardDto {

    private Long userId;

    private String name;

    private Integer xp;

    private Integer streak;

    private Integer rank;

    private boolean currentUser;

}
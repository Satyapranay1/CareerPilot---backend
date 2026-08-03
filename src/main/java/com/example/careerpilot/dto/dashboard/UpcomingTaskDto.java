package com.example.careerpilot.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingTaskDto {

    private String task;

    private String priority;

    private LocalDate dueDate;

    private Integer progress;

}
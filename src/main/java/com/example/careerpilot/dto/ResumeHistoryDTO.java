package com.example.careerpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeHistoryDTO {

    private Long id;

    private String fileName;

    private String company;

    private String jobRole;

    private BigDecimal atsScore;

    private LocalDateTime uploadedAt;

}
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
public class ResumeAnalysisDTO {

    private Long id;

    private String fileName;

    private String company;

    private String jobRole;

    private String knowledgeSource;

    private BigDecimal atsScore;

    private LocalDateTime uploadedAt;

    private ResumeResponse analysis;

}
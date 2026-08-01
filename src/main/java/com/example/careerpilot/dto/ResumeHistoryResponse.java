package com.example.careerpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeHistoryResponse {

    private Long id;

    private String company;

    private String role;

    private String originalFilename;

    private int atsCompatibilityScore;

    private LocalDateTime createdAt;
}
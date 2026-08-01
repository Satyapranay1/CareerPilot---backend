package com.example.careerpilot.dto;

import com.example.careerpilot.model.Difficulty;
import com.example.careerpilot.model.InterviewStatus;
import com.example.careerpilot.model.InterviewType;

import java.time.LocalDateTime;

public record InterviewResponse(

        Long id,

        String companyName,

        String companyWebsite,

        String jobRole,

        InterviewType interviewType,

        Difficulty difficulty,

        InterviewStatus status,

        Double overallScore,

        LocalDateTime createdAt,

        LocalDateTime completedAt

) {}
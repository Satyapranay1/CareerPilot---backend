package com.example.careerpilot.dto;

public record InterviewReportResponse(

        Long interviewId,

        Double overallScore,

        Integer questionsAnswered,

        String strengths,

        String improvements,

        String recommendation

) {}
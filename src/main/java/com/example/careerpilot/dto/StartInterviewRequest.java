package com.example.careerpilot.dto;

import com.example.careerpilot.model.Difficulty;
import com.example.careerpilot.model.InterviewType;
import lombok.Data;

@Data
public class StartInterviewRequest {

    private String companyName;

    private String companyWebsite;

    private String jobRole;

    private String jobDescription;

    private InterviewType interviewType;

    private Difficulty difficulty;
}
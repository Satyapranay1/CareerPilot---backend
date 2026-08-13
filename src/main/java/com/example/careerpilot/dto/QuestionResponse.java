package com.example.careerpilot.dto;

import com.example.careerpilot.model.InterviewType;

public record QuestionResponse(

        Long id,

        String question,

        InterviewType type,

        String topic,

        Integer questionNumber

) {}
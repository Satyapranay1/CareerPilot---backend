package com.example.careerpilot.dto;

public record AnswerEvaluationResponse(

        Long questionId,

        String userAnswer,

        Double score,

        Double correctness,

        Double completeness,

        Double clarity,

        Double depth,

        Double relevance,

        Double starSituation,

        Double starTask,

        Double starAction,

        Double starResult,

        String strengths,

        String missingConcepts,

        String feedback,

        String suggestedAnswer

) {}
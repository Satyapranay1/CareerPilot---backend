package com.example.careerpilot.dto;

import com.example.careerpilot.model.CodingQuestion;

import java.util.List;

public record CodingResponse(
        long solved,
        long total,
        double percentage,
        List<Topic> topics
) {

    public record Topic(
            Long id,
            String name,
            long solved,
            long total
    ) {}

    public record TopicDetails(
            Long id,
            String name,
            long solved,
            long total,
            List<Component> components
    ) {}

    public record Component(
            Long id,
            String name,
            long solved,
            long total,
            List<Question> questions
    ) {}

    public record Question(
            Long id,
            String title,
            CodingQuestion.Difficulty difficulty,
            String[] companies,
            CodingQuestion.Platform platform,
            String problemUrl,
            boolean solved
    ) {}

    public record QuestionPage(
            List<Question> questions,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record DifficultyProgress(
            long easySolved,
            long easyTotal,
            long mediumSolved,
            long mediumTotal,
            long hardSolved,
            long hardTotal
    ) {}

    public record Progress(
            long solved,
            long total,
            double percentage,
            DifficultyProgress difficulty,
            List<Topic> topics
    ) {}
}
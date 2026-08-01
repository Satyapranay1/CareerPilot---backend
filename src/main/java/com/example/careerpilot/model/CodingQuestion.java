package com.example.careerpilot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "coding_questions")
@Getter
@Setter
public class CodingQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private CodingTopic topic;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Column(
            name = "companies",
            columnDefinition = "text[]",
            nullable = false
    )
    private String[] companies;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @Column(
            name = "problem_url",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String problemUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }

    public enum Platform {
        LEETCODE,
        GFG
    }
}
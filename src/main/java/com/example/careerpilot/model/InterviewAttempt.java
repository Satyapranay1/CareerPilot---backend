package com.example.careerpilot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "question_id",
            nullable = false,
            unique = true
    )
    private InterviewQuestion question;


    @Column(
            name = "user_answer",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String userAnswer;


    
    
    

    @Column(name = "score")
    private Double score;


    
    
    

    @Column(name = "correctness")
    private Double correctness;

    @Column(name = "completeness")
    private Double completeness;

    @Column(name = "clarity")
    private Double clarity;

    @Column(name = "depth")
    private Double depth;

    @Column(name = "relevance")
    private Double relevance;


    
    
    

    @Column(name = "star_situation")
    private Double starSituation;

    @Column(name = "star_task")
    private Double starTask;

    @Column(name = "star_action")
    private Double starAction;

    @Column(name = "star_result")
    private Double starResult;


    
    
    

    @Column(
            name = "strengths",
            columnDefinition = "TEXT"
    )
    private String strengths;


    @Column(
            name = "missing_concepts",
            columnDefinition = "TEXT"
    )
    private String missingConcepts;


    @Column(
            name = "feedback",
            columnDefinition = "TEXT"
    )
    private String feedback;


    @Column(
            name = "suggested_answer",
            columnDefinition = "TEXT"
    )
    private String suggestedAnswer;


    @Column(
            name = "answered_at",
            nullable = false
    )
    private LocalDateTime answeredAt;


    @PrePersist
    public void prePersist() {

        if (answeredAt == null) {
            answeredAt = LocalDateTime.now();
        }
    }
}
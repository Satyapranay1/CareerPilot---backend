package com.example.careerpilot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "interview_questions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_question_number",
                        columnNames = {
                                "session_id",
                                "question_number"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "session_id",
            nullable = false
    )
    private InterviewSession session;


    @Column(
            name = "question",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String question;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "question_type",
            nullable = false,
            length = 30
    )
    private InterviewType questionType;


    @Column(name = "topic")
    private String topic;


    @Column(
            name = "question_number",
            nullable = false
    )
    private Integer questionNumber;


    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;


    @PrePersist
    public void prePersist() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
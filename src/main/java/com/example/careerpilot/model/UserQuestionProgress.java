package com.example.careerpilot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_question_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_question_progress",
                        columnNames = {"user_id", "question_id"}
                )
        }
)
@Getter
@Setter
public class UserQuestionProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private CodingQuestion question;

    @Column(name = "solved_at", nullable = false)
    private LocalDateTime solvedAt;

    protected UserQuestionProgress() {
    }

    public UserQuestionProgress(
            User user,
            CodingQuestion question,
            LocalDateTime solvedAt) {

        this.user = user;
        this.question = question;
        this.solvedAt = solvedAt;
    }
}
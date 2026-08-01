package com.example.careerpilot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_knowledge_id")
    private CompanyKnowledge companyKnowledge;


    @Column(name = "company_name")
    private String companyName;


    @Column(
            name = "company_website",
            length = 1000
    )
    private String companyWebsite;


    @Column(
            name = "job_role",
            nullable = false
    )
    private String jobRole;


    @Column(
            name = "job_description",
            columnDefinition = "TEXT"
    )
    private String jobDescription;


    @Column(
            name = "job_description_hash",
            length = 64
    )
    private String jobDescriptionHash;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "interview_type",
            nullable = false,
            length = 30
    )
    private InterviewType interviewType;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "difficulty",
            nullable = false,
            length = 20
    )
    private Difficulty difficulty;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private InterviewStatus status;


    @Column(name = "overall_score")
    private Double overallScore;


    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;


    @Column(name = "completed_at")
    private LocalDateTime completedAt;


    @PrePersist
    public void prePersist() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = InterviewStatus.IN_PROGRESS;
        }
    }
}
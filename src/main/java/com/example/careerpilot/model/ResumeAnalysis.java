package com.example.careerpilot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "resume_analyses",
        indexes = {
                @Index(
                        name = "idx_resume_analyses_user_created",
                        columnList = "user_id, created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String company;

    @Column(nullable = false, length = 150)
    private String role;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_file_path", nullable = false, length = 500)
    private String storedFilePath;

    @Column(
            name = "extracted_text",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String extractedText;

    @Column(name = "ats_score", nullable = false)
    private Integer atsScore;

    @Column(name = "keyword_match_score", nullable = false)
    private Integer keywordMatchScore;

    @Column(name = "impact_score", nullable = false)
    private Integer impactScore;

    @Column(name = "readability_score", nullable = false)
    private Integer readabilityScore;

    @Column(name = "grammar_score", nullable = false)
    private Integer grammarScore;

    @Column(name = "structure_score", nullable = false)
    private Integer structureScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "matched_keywords",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String matchedKeywords = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "missing_keywords",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String missingKeywords = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "weak_keywords",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String weakKeywords = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "strong_areas",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String strongAreas = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "suggestions",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String suggestions = "[]";

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
package com.example.careerpilot.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "company")
    private String company;

    @Column(name = "job_role")
    private String jobRole;

    @Lob
    @Column(name = "resume_text", nullable = false, columnDefinition = "TEXT")
    private String resumeText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode analysisJson;

    @Column(name = "ats_score")
    private BigDecimal atsScore;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
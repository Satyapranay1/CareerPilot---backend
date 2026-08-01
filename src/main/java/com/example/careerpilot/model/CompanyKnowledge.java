package com.example.careerpilot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "company_knowledge",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_normalized_website",
                        columnNames = "normalized_website"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "company_name",
            nullable = false
    )
    private String companyName;

    @Column(
            name = "website",
            nullable = false,
            length = 1000
    )
    private String website;

    @Column(
            name = "normalized_website",
            nullable = false,
            unique = true,
            length = 1000
    )
    private String normalizedWebsite;

    @Column(
            name = "content_hash",
            length = 64
    )
    private String contentHash;

    @Column(
            name = "indexed",
            nullable = false
    )
    private Boolean indexed = false;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

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

        if (indexed == null) {
            indexed = false;
        }
    }
}
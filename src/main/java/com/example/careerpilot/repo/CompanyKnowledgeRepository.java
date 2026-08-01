package com.example.careerpilot.repo;

import com.example.careerpilot.model.CompanyKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyKnowledgeRepository
        extends JpaRepository<CompanyKnowledge, Long> {

    Optional<CompanyKnowledge> findByNormalizedWebsite(
            String normalizedWebsite
    );

    boolean existsByNormalizedWebsite(
            String normalizedWebsite
    );
}
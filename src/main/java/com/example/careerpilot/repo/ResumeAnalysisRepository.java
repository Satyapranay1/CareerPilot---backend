package com.example.careerpilot.repo;

import com.example.careerpilot.model.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeAnalysisRepository
        extends JpaRepository<ResumeAnalysis, Long> {

    List<ResumeAnalysis> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ResumeAnalysis> findByIdAndUserId(
            Long id,
            Long userId
    );
}
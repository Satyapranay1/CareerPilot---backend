package com.example.careerpilot.repo;

import com.example.careerpilot.model.InterviewSession;
import com.example.careerpilot.model.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionRepository
        extends JpaRepository<InterviewSession, Long> {

    Optional<InterviewSession> findByIdAndUserId(
            Long id,
            Long userId
    );

    List<InterviewSession>
    findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    List<InterviewSession>
    findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            InterviewStatus status
    );

    long countByUserIdAndStatus(
            Long userId,
            InterviewStatus status
    );
}
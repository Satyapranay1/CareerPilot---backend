package com.example.careerpilot.repo;

import com.example.careerpilot.model.InterviewAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewAttemptRepository
        extends JpaRepository<InterviewAttempt, Long> {

    Optional<InterviewAttempt> findByQuestionId(
            Long questionId
    );

    List<InterviewAttempt>
    findByQuestionSessionIdOrderByAnsweredAtAsc(
            Long sessionId
    );

    long countByQuestionSessionId(
            Long sessionId
    );

    boolean existsByQuestionId(
            Long questionId
    );
}
package com.example.careerpilot.repo;

import com.example.careerpilot.model.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewQuestionRepository
        extends JpaRepository<InterviewQuestion, Long> {

    Optional<InterviewQuestion>
    findByIdAndSessionId(
            Long id,
            Long sessionId
    );

    List<InterviewQuestion>
    findBySessionIdOrderByQuestionNumberAsc(
            Long sessionId
    );

    long countBySessionId(
            Long sessionId
    );

    boolean existsBySessionIdAndQuestionNumber(
            Long sessionId,
            Integer questionNumber
    );
}
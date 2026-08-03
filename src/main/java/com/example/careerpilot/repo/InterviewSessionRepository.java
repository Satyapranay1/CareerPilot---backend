package com.example.careerpilot.repo;

import com.example.careerpilot.model.InterviewSession;
import com.example.careerpilot.model.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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



    @Query("""
SELECT COALESCE(AVG(i.overallScore),0)
FROM InterviewSession i
WHERE i.user.id = :userId
AND i.status = com.example.careerpilot.model.InterviewStatus.COMPLETED
""")
    Double findAverageScore(Long userId);
    @Query("""
SELECT i
FROM InterviewSession i
WHERE i.user.id=:userId
AND i.status = com.example.careerpilot.model.InterviewStatus.COMPLETED
ORDER BY i.createdAt ASC
""")
    List<InterviewSession> findCompleted(Long userId);
}
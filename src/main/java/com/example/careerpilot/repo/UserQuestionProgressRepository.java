package com.example.careerpilot.repo;

import com.example.careerpilot.model.CodingQuestion;
import com.example.careerpilot.model.UserQuestionProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface UserQuestionProgressRepository
        extends JpaRepository<UserQuestionProgress, Long> {

    boolean existsByUserIdAndQuestionId(
            Long userId,
            Long questionId
    );

    long countByUserId(Long userId);

    @Query("""
            SELECT p.question.id
            FROM UserQuestionProgress p
            WHERE p.user.id = :userId
            AND p.question.topic.id IN :topicIds
            """)
    Set<Long> findSolvedQuestionIdsByTopics(
            @Param("userId") Long userId,
            @Param("topicIds") Collection<Long> topicIds
    );

    @Query("""
            SELECT p.question.id
            FROM UserQuestionProgress p
            WHERE p.user.id = :userId
            """)
    Set<Long> findSolvedQuestionIds(
            @Param("userId") Long userId
    );

    @Query("""
            SELECT parent.id, COUNT(p.id)
            FROM UserQuestionProgress p
            JOIN p.question q
            JOIN q.topic component
            JOIN component.parent parent
            WHERE p.user.id = :userId
            GROUP BY parent.id
            """)
    List<Object[]> countSolvedByRootTopic(
            @Param("userId") Long userId
    );

    @Query("""
SELECT DATE(p.solvedAt),COUNT(p.id)
FROM UserQuestionProgress p
WHERE p.user.id=:userId
GROUP BY DATE(p.solvedAt)
ORDER BY DATE(p.solvedAt)
""")
    List<Object[]> weeklySolved(Long userId);

    List<UserQuestionProgress>
    findByUserIdOrderBySolvedAtAsc(Long userId);

    @Query("""
            SELECT COUNT(p.id)
            FROM UserQuestionProgress p
            WHERE p.user.id = :userId
            AND p.question.difficulty = :difficulty
            """)
    long countSolvedByDifficulty(
            @Param("userId") Long userId,
            @Param("difficulty")
            CodingQuestion.Difficulty difficulty
    );

    @Modifying
    @Query("""
            DELETE FROM UserQuestionProgress p
            WHERE p.user.id = :userId
            AND p.question.id = :questionId
            """)
    int deleteByUserIdAndQuestionId(
            @Param("userId") Long userId,
            @Param("questionId") Long questionId
    );

    List<UserQuestionProgress> findByUserIdOrderBySolvedAtDesc(Long userId);
}
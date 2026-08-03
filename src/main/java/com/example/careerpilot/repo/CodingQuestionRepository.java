package com.example.careerpilot.repo;

import com.example.careerpilot.model.CodingQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface CodingQuestionRepository
        extends JpaRepository<CodingQuestion, Long>,
        JpaSpecificationExecutor<CodingQuestion> {

    List<CodingQuestion>
    findByTopicIdInOrderByDisplayOrderAsc(
            Collection<Long> topicIds
    );

    long count();

    @Query("""
            SELECT parent.id, COUNT(q.id)
            FROM CodingQuestion q
            JOIN q.topic component
            JOIN component.parent parent
            GROUP BY parent.id
            """)
    List<Object[]> countQuestionsByRootTopic();

    long countByDifficulty(
            CodingQuestion.Difficulty difficulty
    );
}
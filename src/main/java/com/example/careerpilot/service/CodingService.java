package com.example.careerpilot.service;

import com.example.careerpilot.dto.CodingResponse;
import com.example.careerpilot.model.CodingQuestion;
import com.example.careerpilot.model.CodingTopic;
import com.example.careerpilot.model.User;
import com.example.careerpilot.model.UserQuestionProgress;
import com.example.careerpilot.repo.CodingQuestionRepository;
import com.example.careerpilot.repo.CodingTopicRepository;
import com.example.careerpilot.repo.UserQuestionProgressRepository;
import com.example.careerpilot.repo.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CodingService {

    private final CodingTopicRepository topicRepository;
    private final CodingQuestionRepository questionRepository;
    private final UserQuestionProgressRepository progressRepository;
    private final UserRepo userRepository;

    public CodingService(
            CodingTopicRepository topicRepository,
            CodingQuestionRepository questionRepository,
            UserQuestionProgressRepository progressRepository,
            UserRepo userRepository) {

        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
    }

    
    
    

    @Transactional(readOnly = true)
    public CodingResponse getCatalog(
            Authentication authentication) {

        User user = getUser(authentication);

        List<CodingTopic> rootTopics =
                topicRepository
                        .findByParentIsNullOrderByDisplayOrderAsc();

        Map<Long, Long> totalByTopic =
                questionRepository
                        .countQuestionsByRootTopic()
                        .stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],
                                row -> (Long) row[1]
                        ));

        Map<Long, Long> solvedByTopic =
                progressRepository
                        .countSolvedByRootTopic(user.getId())
                        .stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],
                                row -> (Long) row[1]
                        ));

        List<CodingResponse.Topic> topics =
                rootTopics.stream()
                        .map(topic ->
                                new CodingResponse.Topic(
                                        topic.getId(),
                                        topic.getName(),
                                        solvedByTopic.getOrDefault(
                                                topic.getId(),
                                                0L
                                        ),
                                        totalByTopic.getOrDefault(
                                                topic.getId(),
                                                0L
                                        )
                                )
                        )
                        .toList();

        long total = questionRepository.count();

        long solved =
                progressRepository
                        .countByUserId(user.getId());

        return new CodingResponse(
                solved,
                total,
                percentage(solved, total),
                topics
        );
    }

    
    
    

    @Transactional(readOnly = true)
    public CodingResponse.TopicDetails getTopic(
            Long topicId,
            Authentication authentication) {

        User user = getUser(authentication);

        CodingTopic topic =
                topicRepository
                        .findById(topicId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Topic not found"
                                )
                        );

        if (topic.getParent() != null) {
            throw new IllegalArgumentException(
                    "Topic ID must belong to a root topic"
            );
        }

        List<CodingTopic> components =
                topicRepository
                        .findByParentIdOrderByDisplayOrderAsc(
                                topicId
                        );

        if (components.isEmpty()) {

            return new CodingResponse.TopicDetails(
                    topic.getId(),
                    topic.getName(),
                    0,
                    0,
                    List.of()
            );
        }

        List<Long> componentIds =
                components.stream()
                        .map(CodingTopic::getId)
                        .toList();

        List<CodingQuestion> questions =
                questionRepository
                        .findByTopicIdInOrderByDisplayOrderAsc(
                                componentIds
                        );

        Set<Long> solvedIds =
                progressRepository
                        .findSolvedQuestionIdsByTopics(
                                user.getId(),
                                componentIds
                        );

        Map<Long, List<CodingQuestion>> questionsByTopic =
                questions.stream()
                        .collect(Collectors.groupingBy(
                                question ->
                                        question.getTopic().getId()
                        ));

        List<CodingResponse.Component> componentResponses =
                components.stream()
                        .map(component ->
                                buildComponent(
                                        component,
                                        questionsByTopic,
                                        solvedIds
                                )
                        )
                        .toList();

        long total =
                componentResponses.stream()
                        .mapToLong(
                                CodingResponse.Component::total
                        )
                        .sum();

        long solved =
                componentResponses.stream()
                        .mapToLong(
                                CodingResponse.Component::solved
                        )
                        .sum();

        return new CodingResponse.TopicDetails(
                topic.getId(),
                topic.getName(),
                solved,
                total,
                componentResponses
        );
    }

    
    
    

    @Transactional(readOnly = true)
    public CodingResponse.QuestionPage getQuestions(
            String search,
            CodingQuestion.Difficulty difficulty,
            CodingQuestion.Platform platform,
            Boolean solved,
            Long topicId,
            String company,
            int page,
            int size,
            Authentication authentication) {

        User user = getUser(authentication);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(
                Math.max(size, 1),
                100
        );

        Set<Long> solvedIds =
                progressRepository
                        .findSolvedQuestionIds(user.getId());

        Specification<CodingQuestion> specification =
                buildSpecification(
                        search,
                        difficulty,
                        platform,
                        solved,
                        topicId,
                        company,
                        solvedIds
                );

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                Sort.Direction.ASC,
                                "id"
                        )
                );

        Page<CodingQuestion> result =
                questionRepository.findAll(
                        specification,
                        pageable
                );

        List<CodingResponse.Question> questions =
                result.getContent()
                        .stream()
                        .map(question ->
                                toQuestionResponse(
                                        question,
                                        solvedIds.contains(
                                                question.getId()
                                        )
                                )
                        )
                        .toList();

        return new CodingResponse.QuestionPage(
                questions,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private Specification<CodingQuestion> buildSpecification(
            String search,
            CodingQuestion.Difficulty difficulty,
            CodingQuestion.Platform platform,
            Boolean solved,
            Long topicId,
            String company,
            Set<Long> solvedIds) {

        return (root, query, cb) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            
            if (search != null &&
                    !search.isBlank()) {

                predicates.add(
                        cb.like(
                                cb.lower(
                                        root.get("title")
                                ),
                                "%" +
                                        search
                                                .trim()
                                                .toLowerCase() +
                                        "%"
                        )
                );
            }

            
            if (difficulty != null) {

                predicates.add(
                        cb.equal(
                                root.get("difficulty"),
                                difficulty
                        )
                );
            }

            
            if (platform != null) {

                predicates.add(
                        cb.equal(
                                root.get("platform"),
                                platform
                        )
                );
            }

            
            if (topicId != null) {

                predicates.add(
                        cb.or(
                                cb.equal(
                                        root
                                                .get("topic")
                                                .get("id"),
                                        topicId
                                ),
                                cb.equal(
                                        root
                                                .get("topic")
                                                .get("parent")
                                                .get("id"),
                                        topicId
                                )
                        )
                );
            }

            

            
            if (solved != null) {

                if (solved) {

                    if (solvedIds.isEmpty()) {

                        predicates.add(
                                cb.disjunction()
                        );

                    } else {

                        predicates.add(
                                root
                                        .get("id")
                                        .in(solvedIds)
                        );
                    }

                } else {

                    if (!solvedIds.isEmpty()) {

                        predicates.add(
                                cb.not(
                                        root
                                                .get("id")
                                                .in(solvedIds)
                                )
                        );
                    }
                }
            }

            return cb.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }

    
    
    

    @Transactional
    public boolean setSolved(
            Long questionId,
            boolean solved,
            Authentication authentication) {

        User user = getUser(authentication);

        CodingQuestion question =
                questionRepository
                        .findById(questionId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Question not found"
                                )
                        );

        boolean exists =
                progressRepository
                        .existsByUserIdAndQuestionId(
                                user.getId(),
                                questionId
                        );

        if (solved && !exists) {

            progressRepository.save(
                    new UserQuestionProgress(
                            user,
                            question,
                            LocalDateTime.now()
                    )
            );

        } else if (!solved && exists) {

            progressRepository
                    .deleteByUserIdAndQuestionId(
                            user.getId(),
                            questionId
                    );
        }

        return solved;
    }

    
    
    

    @Transactional(readOnly = true)
    public CodingResponse.Progress getProgress(
            Authentication authentication) {

        User user = getUser(authentication);

        long total =
                questionRepository.count();

        long solved =
                progressRepository
                        .countByUserId(user.getId());

        long easyTotal =
                questionRepository.countByDifficulty(
                        CodingQuestion.Difficulty.EASY
                );

        long mediumTotal =
                questionRepository.countByDifficulty(
                        CodingQuestion.Difficulty.MEDIUM
                );

        long hardTotal =
                questionRepository.countByDifficulty(
                        CodingQuestion.Difficulty.HARD
                );

        long easySolved =
                progressRepository
                        .countSolvedByDifficulty(
                                user.getId(),
                                CodingQuestion.Difficulty.EASY
                        );

        long mediumSolved =
                progressRepository
                        .countSolvedByDifficulty(
                                user.getId(),
                                CodingQuestion.Difficulty.MEDIUM
                        );

        long hardSolved =
                progressRepository
                        .countSolvedByDifficulty(
                                user.getId(),
                                CodingQuestion.Difficulty.HARD
                        );

        List<CodingTopic> rootTopics =
                topicRepository
                        .findByParentIsNullOrderByDisplayOrderAsc();

        Map<Long, Long> totalByTopic =
                questionRepository
                        .countQuestionsByRootTopic()
                        .stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],
                                row -> (Long) row[1]
                        ));

        Map<Long, Long> solvedByTopic =
                progressRepository
                        .countSolvedByRootTopic(
                                user.getId()
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],
                                row -> (Long) row[1]
                        ));

        List<CodingResponse.Topic> topics =
                rootTopics.stream()
                        .map(topic ->
                                new CodingResponse.Topic(
                                        topic.getId(),
                                        topic.getName(),
                                        solvedByTopic.getOrDefault(
                                                topic.getId(),
                                                0L
                                        ),
                                        totalByTopic.getOrDefault(
                                                topic.getId(),
                                                0L
                                        )
                                )
                        )
                        .toList();

        CodingResponse.DifficultyProgress difficulty =
                new CodingResponse.DifficultyProgress(
                        easySolved,
                        easyTotal,
                        mediumSolved,
                        mediumTotal,
                        hardSolved,
                        hardTotal
                );

        return new CodingResponse.Progress(
                solved,
                total,
                percentage(solved, total),
                difficulty,
                topics
        );
    }

    
    
    

    private CodingResponse.Component buildComponent(
            CodingTopic component,
            Map<Long, List<CodingQuestion>> questionsByTopic,
            Set<Long> solvedIds) {

        List<CodingResponse.Question> questions =
                questionsByTopic
                        .getOrDefault(
                                component.getId(),
                                List.of()
                        )
                        .stream()
                        .map(question ->
                                toQuestionResponse(
                                        question,
                                        solvedIds.contains(
                                                question.getId()
                                        )
                                )
                        )
                        .toList();

        long solved =
                questions.stream()
                        .filter(
                                CodingResponse.Question::solved
                        )
                        .count();

        return new CodingResponse.Component(
                component.getId(),
                component.getName(),
                solved,
                questions.size(),
                questions
        );
    }

    private CodingResponse.Question toQuestionResponse(
            CodingQuestion question,
            boolean solved) {

        return new CodingResponse.Question(
                question.getId(),
                question.getTitle(),
                question.getDifficulty(),
                question.getCompanies(),
                question.getPlatform(),
                question.getProblemUrl(),
                solved
        );
    }

    private double percentage(
            long solved,
            long total) {

        if (total == 0) {
            return 0;
        }

        return Math.round(
                ((double) solved / total)
                        * 10000.0
        ) / 100.0;
    }

    private User getUser(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }

        return userRepository
                .findByEmail(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Authenticated user not found"
                        )
                );
    }
}
package com.example.careerpilot.service;

import com.example.careerpilot.dto.*;
import com.example.careerpilot.exception.InterviewNotFoundException;
import com.example.careerpilot.model.*;
import com.example.careerpilot.repo.InterviewAttemptRepository;
import com.example.careerpilot.repo.InterviewQuestionRepository;
import com.example.careerpilot.repo.InterviewSessionRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAttemptRepository attemptRepository;

    private final RagIngestionService ragIngestionService;
    private final InterviewAiService interviewAiService;

    private final ObjectMapper objectMapper;


    // ==========================================
    // START INTERVIEW
    // ==========================================

    @Transactional
    public InterviewResponse startInterview(
            User user,
            StartInterviewRequest request
    ) {

        validateStartRequest(request);

        CompanyKnowledge companyKnowledge =
                ragIngestionService.prepareCompany(
                        request.getCompanyName(),
                        request.getCompanyWebsite()
                );

        InterviewSession session =
                InterviewSession.builder()

                        .user(user)

                        .companyKnowledge(companyKnowledge)

                        .companyName(
                                clean(request.getCompanyName())
                        )

                        .companyWebsite(
                                clean(request.getCompanyWebsite())
                        )

                        .jobRole(
                                clean(request.getJobRole())
                        )

                        .jobDescription(
                                clean(request.getJobDescription())
                        )

                        .interviewType(
                                request.getInterviewType()
                        )

                        .difficulty(
                                request.getDifficulty()
                        )

                        .status(
                                InterviewStatus.IN_PROGRESS
                        )

                        .build();

        session = sessionRepository.save(session);

        String jobDescriptionHash =
                ragIngestionService
                        .ingestJobDescription(session);

        session.setJobDescriptionHash(
                jobDescriptionHash
        );

        session = sessionRepository.save(session);

        return mapSession(session);
    }


    // ==========================================
    // GENERATE NEXT QUESTION
    // ==========================================

    @Transactional
    public QuestionResponse generateQuestion(
            Long sessionId,
            User user
    ) {

        InterviewSession session =
                getActiveSession(
                        sessionId,
                        user
                );

        List<InterviewQuestion> existingQuestions =
                questionRepository
                        .findBySessionIdOrderByQuestionNumberAsc(
                                sessionId
                        );

        int questionNumber =
                existingQuestions.size() + 1;


        // ======================================
        // QUESTION 1
        // ======================================

        if (questionNumber == 1) {

            InterviewQuestion question =
                    InterviewQuestion.builder()

                            .session(session)

                            .question(
                                    "Tell me about yourself."
                            )

                            .questionType(
                                    InterviewType.BEHAVIOURAL
                            )

                            .topic(
                                    "Introduction"
                            )

                            .questionNumber(1)

                            .build();

            question =
                    questionRepository.save(question);

            return mapQuestion(question);
        }


        // ======================================
        // Q2+ REQUIRES PREVIOUS ANSWER
        // ======================================

        InterviewQuestion previousQuestion =
                existingQuestions.get(
                        existingQuestions.size() - 1
                );

        InterviewAttempt previousAttempt =
                attemptRepository
                        .findByQuestionId(
                                previousQuestion.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Answer the current question before generating the next question"
                                        )
                        );


        // ======================================
        // DETERMINE TYPE
        // ======================================

        InterviewType questionType =
                determineQuestionType(
                        session,
                        questionNumber
                );


        // ======================================
        // BUILD CONVERSATION HISTORY
        // ======================================

        String interviewHistory =
                buildInterviewHistory(
                        sessionId
                );


        // ======================================
        // GENERATE ADAPTIVE QUESTION
        // ======================================

        String generatedQuestion =
                interviewAiService
                        .generateQuestion(
                                session,
                                questionType,
                                questionNumber,
                                interviewHistory,
                                previousAttempt.getUserAnswer()
                        );


        if (generatedQuestion == null
                || generatedQuestion.isBlank()) {

            throw new RuntimeException(
                    "AI failed to generate question"
            );
        }


        InterviewQuestion question =
                InterviewQuestion.builder()

                        .session(session)

                        .question(
                                generatedQuestion.trim()
                        )

                        .questionType(
                                questionType
                        )

                        .questionNumber(
                                questionNumber
                        )

                        .build();


        question =
                questionRepository.save(question);


        return mapQuestion(question);
    }


    // ==========================================
    // BUILD INTERVIEW HISTORY
    // ==========================================

    private String buildInterviewHistory(
            Long sessionId
    ) {

        List<InterviewQuestion> questions =
                questionRepository
                        .findBySessionIdOrderByQuestionNumberAsc(
                                sessionId
                        );

        if (questions.isEmpty()) {
            return "No previous interview history.";
        }


        StringBuilder history =
                new StringBuilder();


        for (InterviewQuestion question : questions) {

            history
                    .append("Question ")
                    .append(
                            question.getQuestionNumber()
                    )
                    .append(":\n")
                    .append(
                            question.getQuestion()
                    )
                    .append("\n");


            attemptRepository
                    .findByQuestionId(
                            question.getId()
                    )
                    .ifPresent(
                            attempt -> {

                                history
                                        .append(
                                                "Candidate Answer:\n"
                                        )

                                        .append(
                                                attempt.getUserAnswer()
                                        )

                                        .append("\n");
                            }
                    );


            history.append("\n");
        }


        return history
                .toString()
                .trim();
    }


    // ==========================================
    // SUBMIT ANSWER
    // ==========================================

    @Transactional
    public AnswerEvaluationResponse submitAnswer(
            Long sessionId,
            Long questionId,
            String answer,
            User user
    ) {

        InterviewSession session =
                getActiveSession(
                        sessionId,
                        user
                );

        InterviewQuestion question =
                questionRepository
                        .findByIdAndSessionId(
                                questionId,
                                sessionId
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Question not found"
                                        )
                        );


        // ==========================================
        // RETURN EXISTING ANSWER
        // ==========================================

        Optional<InterviewAttempt> existingAttempt =
                attemptRepository
                        .findByQuestionId(
                                questionId
                        );

        if (existingAttempt.isPresent()) {

            return mapAttempt(
                    existingAttempt.get()
            );
        }


        // ==========================================
        // VALIDATE ANSWER
        // ==========================================

        if (answer == null
                || answer.isBlank()) {

            throw new IllegalArgumentException(
                    "Answer cannot be empty"
            );
        }


        String cleanedAnswer =
                answer.trim();


        // ==========================================
        // AI EVALUATION
        // ==========================================

        String evaluation;

        if (question.getQuestionType()
                == InterviewType.BEHAVIOURAL) {

            evaluation =
                    interviewAiService
                            .evaluateBehavioural(
                                    session,
                                    question,
                                    cleanedAnswer
                            );

        } else {

            evaluation =
                    interviewAiService
                            .evaluateTechnical(
                                    session,
                                    question,
                                    cleanedAnswer
                            );
        }


        JsonNode json =
                parseEvaluation(
                        evaluation
                );


        // ==========================================
        // EXTRACT COMPONENT SCORES
        // ==========================================

        Double correctness =
                score(
                        json,
                        "correctness"
                );

        Double completeness =
                score(
                        json,
                        "completeness"
                );

        Double clarity =
                score(
                        json,
                        "clarity"
                );

        Double depth =
                score(
                        json,
                        "depth"
                );

        Double relevance =
                score(
                        json,
                        "relevance"
                );

        Double starSituation =
                score(
                        json,
                        "starSituation"
                );

        Double starTask =
                score(
                        json,
                        "starTask"
                );

        Double starAction =
                score(
                        json,
                        "starAction"
                );

        Double starResult =
                score(
                        json,
                        "starResult"
                );


        // ==========================================
        // CALCULATE SCORE IN JAVA
        // ==========================================

        double overallScore;

        if (question.getQuestionType()
                == InterviewType.TECHNICAL) {

            overallScore =
                    calculateAverage(
                            correctness,
                            completeness,
                            clarity,
                            depth,
                            relevance
                    );

        } else {

            overallScore =
                    calculateBehaviouralScore(
                            question,
                            clarity,
                            relevance,
                            starSituation,
                            starTask,
                            starAction,
                            starResult
                    );
        }


        // ==========================================
        // CREATE ATTEMPT
        // ==========================================

        InterviewAttempt attempt =
                InterviewAttempt.builder()

                        .question(question)

                        .userAnswer(
                                cleanedAnswer
                        )

                        .score(
                                overallScore
                        )

                        .correctness(
                                correctness
                        )

                        .completeness(
                                completeness
                        )

                        .clarity(
                                clarity
                        )

                        .depth(
                                depth
                        )

                        .relevance(
                                relevance
                        )

                        .starSituation(
                                starSituation
                        )

                        .starTask(
                                starTask
                        )

                        .starAction(
                                starAction
                        )

                        .starResult(
                                starResult
                        )

                        .strengths(
                                text(
                                        json,
                                        "strengths"
                                )
                        )

                        .missingConcepts(
                                text(
                                        json,
                                        "missingConcepts"
                                )
                        )

                        .feedback(
                                text(
                                        json,
                                        "feedback"
                                )
                        )

                        .suggestedAnswer(
                                text(
                                        json,
                                        "suggestedAnswer"
                                )
                        )

                        .build();


        attempt =
                attemptRepository.save(
                        attempt
                );


        return mapAttempt(attempt);
    }


    // ==========================================
    // BEHAVIOURAL SCORE
    // ==========================================

    private double calculateBehaviouralScore(
            InterviewQuestion question,
            Double clarity,
            Double relevance,
            Double starSituation,
            Double starTask,
            Double starAction,
            Double starResult
    ) {

        /*
         * Introduction is NOT a STAR question.
         *
         * For Q1:
         *
         * Tell me about yourself
         *
         * score using clarity + relevance.
         *
         * The AI evaluates experience presentation,
         * skill alignment and career direction in
         * its feedback.
         */

        if (isIntroductionQuestion(question)) {

            return calculateAverage(
                    clarity,
                    relevance
            );
        }


        /*
         * Normal behavioural questions use:
         *
         * STAR + clarity + relevance.
         *
         * Null values are ignored.
         */

        return calculateAverage(
                starSituation,
                starTask,
                starAction,
                starResult,
                clarity,
                relevance
        );
    }


    // ==========================================
    // INTRODUCTION DETECTION
    // ==========================================

    private boolean isIntroductionQuestion(
            InterviewQuestion question
    ) {

        if (question == null) {
            return false;
        }


        if (question.getTopic() != null
                && question.getTopic()
                .equalsIgnoreCase(
                        "Introduction"
                )) {

            return true;
        }


        String value =
                question.getQuestion();


        if (value == null) {
            return false;
        }


        String normalized =
                value
                        .trim()
                        .toLowerCase();


        return normalized.contains(
                "tell me about yourself"
        );
    }


    // ==========================================
    // AVERAGE NON-NULL SCORES
    // ==========================================

    private double calculateAverage(
            Double... scores
    ) {

        if (scores == null
                || scores.length == 0) {

            return 0.0;
        }


        double total = 0.0;

        int count = 0;


        for (Double value : scores) {

            if (value != null) {

                total += value;

                count++;
            }
        }


        if (count == 0) {

            return 0.0;
        }


        return round(
                total / count
        );
    }


    // ==========================================
    // MANUAL FOLLOW-UP
    // ==========================================

    @Transactional
    public QuestionResponse generateFollowUp(
            Long sessionId,
            Long questionId,
            User user
    ) {

        InterviewSession session =
                getActiveSession(
                        sessionId,
                        user
                );


        InterviewQuestion previousQuestion =
                questionRepository

                        .findByIdAndSessionId(
                                questionId,
                                sessionId
                        )

                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "Question not found"
                                        )
                        );


        InterviewAttempt attempt =
                attemptRepository

                        .findByQuestionId(
                                questionId
                        )

                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Answer the question before generating a follow-up"
                                        )
                        );


        String followUp =
                interviewAiService
                        .generateFollowUp(
                                session,
                                previousQuestion,
                                attempt.getUserAnswer()
                        );


        if (followUp == null
                || followUp.isBlank()) {

            throw new RuntimeException(
                    "AI failed to generate follow-up"
            );
        }


        int questionNumber =
                Math.toIntExact(

                        questionRepository
                                .countBySessionId(
                                        sessionId
                                )

                                + 1
                );


        InterviewQuestion question =
                InterviewQuestion.builder()

                        .session(session)

                        .question(
                                followUp.trim()
                        )

                        .questionType(
                                previousQuestion
                                        .getQuestionType()
                        )

                        .topic(
                                previousQuestion
                                        .getTopic()
                        )

                        .questionNumber(
                                questionNumber
                        )

                        .build();


        question =
                questionRepository.save(
                        question
                );


        return mapQuestion(question);
    }


    // ==========================================
    // COMPLETE INTERVIEW
    // ==========================================

    @Transactional
    public InterviewReportResponse completeInterview(
            Long sessionId,
            User user
    ) {

        InterviewSession session =
                getActiveSession(
                        sessionId,
                        user
                );


        List<InterviewAttempt> attempts =
                attemptRepository
                        .findByQuestionSessionIdOrderByAnsweredAtAsc(
                                sessionId
                        );


        if (attempts.isEmpty()) {

            throw new IllegalStateException(
                    "Cannot complete an interview without answering any questions"
            );
        }


        double overallScore =
                attempts
                        .stream()

                        .filter(
                                attempt ->
                                        attempt.getScore()
                                                != null
                        )

                        .mapToDouble(
                                InterviewAttempt::getScore
                        )

                        .average()

                        .orElse(0.0);


        overallScore =
                round(overallScore);


        session.setOverallScore(
                overallScore
        );

        session.setStatus(
                InterviewStatus.COMPLETED
        );

        session.setCompletedAt(
                LocalDateTime.now()
        );


        sessionRepository.save(session);


        String strengths =
                attempts
                        .stream()

                        .map(
                                InterviewAttempt::getStrengths
                        )

                        .filter(
                                value ->
                                        value != null
                                                && !value.isBlank()
                        )

                        .distinct()

                        .limit(5)

                        .reduce(
                                "",
                                this::appendLine
                        )

                        .trim();


        String improvements =
                attempts
                        .stream()

                        .map(
                                InterviewAttempt::getMissingConcepts
                        )

                        .filter(
                                value ->
                                        value != null
                                                && !value.isBlank()
                        )

                        .distinct()

                        .limit(5)

                        .reduce(
                                "",
                                this::appendLine
                        )

                        .trim();


        String recommendation =
                buildRecommendation(
                        overallScore
                );


        return new InterviewReportResponse(

                session.getId(),

                overallScore,

                attempts.size(),

                strengths,

                improvements,

                recommendation
        );
    }


    // ==========================================
    // HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<InterviewResponse> getHistory(
            User user
    ) {

        return sessionRepository

                .findByUserIdOrderByCreatedAtDesc(
                        user.getId()
                )

                .stream()

                .map(this::mapSession)

                .toList();
    }


    // ==========================================
    // GET INTERVIEW
    // ==========================================

    @Transactional(readOnly = true)
    public InterviewResponse getInterview(
            Long sessionId,
            User user
    ) {

        InterviewSession session =
                getSession(
                        sessionId,
                        user
                );


        return mapSession(session);
    }


    // ==========================================
    // GET QUESTIONS
    // ==========================================

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestions(
            Long sessionId,
            User user
    ) {

        getSession(
                sessionId,
                user
        );


        return questionRepository

                .findBySessionIdOrderByQuestionNumberAsc(
                        sessionId
                )

                .stream()

                .map(this::mapQuestion)

                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnswerEvaluationResponse> getAnswers(
            Long sessionId,
            User user
    ) {
        getSession(sessionId, user);

        return attemptRepository
                .findByQuestionSessionIdOrderByAnsweredAtAsc(sessionId)
                .stream()
                .map(this::mapAttempt)
                .toList();
    }


    // ==========================================
    // DETERMINE QUESTION TYPE
    // ==========================================

    private InterviewType determineQuestionType(
            InterviewSession session,
            int questionNumber
    ) {

        InterviewType type =
                session.getInterviewType();


        if (questionNumber == 1) {

            return InterviewType.BEHAVIOURAL;
        }


        if (type == InterviewType.TECHNICAL) {

            return InterviewType.TECHNICAL;
        }


        if (type == InterviewType.BEHAVIOURAL) {

            return InterviewType.BEHAVIOURAL;
        }


        /*
         * MIXED
         *
         * Q1 -> Behavioural introduction
         * Q2 -> Technical
         * Q3 -> Technical
         * Q4 -> Behavioural
         * Q5 -> Technical
         * Q6 -> Technical
         * Q7 -> Behavioural
         */

        return (questionNumber - 1) % 3 == 0

                ? InterviewType.BEHAVIOURAL

                : InterviewType.TECHNICAL;
    }


    // ==========================================
    // SESSION OWNERSHIP
    // ==========================================

    private InterviewSession getSession(
            Long sessionId,
            User user
    ) {

        return sessionRepository

                .findByIdAndUserId(
                        sessionId,
                        user.getId()
                )

                .orElseThrow(
                        () ->
                                new InterviewNotFoundException(
                                        sessionId
                                )
                );
    }


    private InterviewSession getActiveSession(
            Long sessionId,
            User user
    ) {

        InterviewSession session =
                getSession(
                        sessionId,
                        user
                );


        if (session.getStatus()
                == InterviewStatus.COMPLETED) {

            throw new IllegalStateException(
                    "Interview has already been completed"
            );
        }


        return session;
    }


    // ==========================================
    // VALIDATION
    // ==========================================

    private void validateStartRequest(
            StartInterviewRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Interview request is required"
            );
        }


        if (request.getJobRole() == null
                || request.getJobRole().isBlank()) {

            throw new IllegalArgumentException(
                    "Job role is required"
            );
        }


        if (request.getJobDescription() == null
                || request.getJobDescription().isBlank()) {

            throw new IllegalArgumentException(
                    "Job description is required"
            );
        }


        if (request.getInterviewType() == null) {

            throw new IllegalArgumentException(
                    "Interview type is required"
            );
        }


        if (request.getDifficulty() == null) {

            throw new IllegalArgumentException(
                    "Difficulty is required"
            );
        }
    }


    // ==========================================
    // AI JSON PARSING
    // ==========================================

    private JsonNode parseEvaluation(String response) {

        if (response == null || response.isBlank()) {
            throw new RuntimeException(
                    "AI returned an empty evaluation"
            );
        }

        try {

            String cleaned = response.trim();

            // Remove markdown code fences
            cleaned = cleaned
                    .replace("```json", "")
                    .replace("```JSON", "")
                    .replace("```", "")
                    .trim();

            // Extract JSON object if AI added text around it
            int firstBrace = cleaned.indexOf('{');
            int lastBrace = cleaned.lastIndexOf('}');

            if (firstBrace < 0 || lastBrace <= firstBrace) {

                throw new RuntimeException(
                        "AI did not return valid JSON. Raw response: "
                                + cleaned
                );
            }

            cleaned = cleaned.substring(
                    firstBrace,
                    lastBrace + 1
            );

            JsonNode json =
                    objectMapper.readTree(cleaned);

            if (json == null || !json.isObject()) {

                throw new RuntimeException(
                        "AI evaluation is not a JSON object"
                );
            }

            return json;

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Unable to parse AI evaluation",
                    exception
            );
        }
    }


    // ==========================================
    // SCORE EXTRACTION
    // ==========================================

    private Double score(
            JsonNode json,
            String field
    ) {

        JsonNode value = json.get(field);

        if (value == null || value.isNull()) {
            return null;
        }

        if (!value.isNumber()) {
            throw new IllegalStateException(
                    "AI returned non-numeric score for: " + field
            );
        }

        double result = value.asDouble();

        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new IllegalStateException(
                    "Invalid score for: " + field
            );
        }

        if (result < 0 || result > 10) {
            throw new IllegalStateException(
                    "Score out of range for "
                            + field
                            + ": "
                            + result
            );
        }

        return round(result);
    }


    private String text(
            JsonNode json,
            String field
    ) {

        JsonNode value =
                json.get(field);


        if (value == null
                || value.isNull()) {

            return null;
        }


        String result =
                value.asText();


        if (result == null) {
            return null;
        }


        result =
                result.trim();


        return result.isEmpty()
                ? null
                : result;
    }


    // ==========================================
    // MAPPERS
    // ==========================================

    private InterviewResponse mapSession(
            InterviewSession session
    ) {

        return new InterviewResponse(

                session.getId(),

                session.getCompanyName(),

                session.getCompanyWebsite(),

                session.getJobRole(),

                session.getInterviewType(),

                session.getDifficulty(),

                session.getStatus(),

                session.getOverallScore(),

                session.getCreatedAt(),

                session.getCompletedAt()
        );
    }


    private QuestionResponse mapQuestion(
            InterviewQuestion question
    ) {

        return new QuestionResponse(

                question.getId(),

                question.getQuestion(),

                question.getQuestionType(),

                question.getTopic(),

                question.getQuestionNumber()
        );
    }


    private AnswerEvaluationResponse mapAttempt(
            InterviewAttempt attempt
    ) {

        return new AnswerEvaluationResponse(

                attempt.getQuestion().getId(),

                attempt.getUserAnswer(),

                attempt.getScore(),

                attempt.getCorrectness(),

                attempt.getCompleteness(),

                attempt.getClarity(),

                attempt.getDepth(),

                attempt.getRelevance(),

                attempt.getStarSituation(),

                attempt.getStarTask(),

                attempt.getStarAction(),

                attempt.getStarResult(),

                attempt.getStrengths(),

                attempt.getMissingConcepts(),

                attempt.getFeedback(),

                attempt.getSuggestedAnswer()
        );
    }


    // ==========================================
    // HELPERS
    // ==========================================

    private String clean(
            String value
    ) {

        if (value == null) {
            return null;
        }


        String cleaned =
                value.trim();


        return cleaned.isEmpty()
                ? null
                : cleaned;
    }


    private double round(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }


    private String appendLine(
            String existing,
            String value
    ) {

        if (existing == null
                || existing.isBlank()) {

            return value;
        }


        return existing
                + "\n"
                + value;
    }


    private String buildRecommendation(
            double score
    ) {

        if (score >= 8.5) {

            return "Strong performance. Focus on deeper follow-up questions and maintaining concise, structured answers.";
        }


        if (score >= 7.0) {

            return "Good foundation. Review the identified gaps and practice explaining concepts with greater depth and specificity.";
        }


        if (score >= 5.0) {

            return "Continue practicing the core requirements from the job description and improve answer structure before attempting harder questions.";
        }


        return "Focus on the fundamentals required by the role, then repeat the interview after practicing the identified weak areas.";
    }
}
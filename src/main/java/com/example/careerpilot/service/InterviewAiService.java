package com.example.careerpilot.service;

import com.example.careerpilot.model.InterviewQuestion;
import com.example.careerpilot.model.InterviewSession;
import com.example.careerpilot.model.InterviewType;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewAiService {

    private static final int JD_TOP_K = 2;
    private static final int COMPANY_TOP_K = 1;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;


    // =========================================================
    // GENERATE NEXT QUESTION
    // =========================================================

    public String generateQuestion(
            InterviewSession session,
            InterviewType questionType,
            int questionNumber,
            String interviewHistory,
            String latestAnswer
    ) {

        String query = safe(session.getJobRole())
                + " "
                + safe(latestAnswer);

        RagContext context = retrieveContext(session, query);

        String prompt = """
                You are a professional job interviewer.

                Role: %s
                Company: %s
                Type: %s
                Difficulty: %s

                Previous interview:
                %s

                Latest answer:
                %s

                Job context:
                %s

                Generate ONE relevant next question.

                Rules:
                - Do not repeat previous questions.
                - Prefer important uncovered job requirements.
                - If the latest answer contains a useful claim,
                  ask a relevant follow-up.
                - Never invent candidate experience.
                - Match the requested interview type and difficulty.
                - Return ONLY the question.
                """
                .formatted(
                        limit(session.getJobRole(), 150),
                        limit(session.getCompanyName(), 150),
                        questionType,
                        session.getDifficulty(),
                        limit(interviewHistory, 2500),
                        limit(latestAnswer, 1000),
                        limit(context.jobDescription(), 1500)
                );

        return cleanQuestion(
                chatClient
                        .prompt()
                        .user(prompt)
                        .call()
                        .content()
        );
    }


    // =========================================================
    // TECHNICAL EVALUATION
    // =========================================================

    public String evaluateTechnical(
            InterviewSession session,
            InterviewQuestion question,
            String userAnswer
    ) {

        return evaluate(
                session,
                question,
                userAnswer,
                "TECHNICAL"
        );
    }


    // =========================================================
    // BEHAVIOURAL EVALUATION
    // =========================================================

    public String evaluateBehavioural(
            InterviewSession session,
            InterviewQuestion question,
            String userAnswer
    ) {

        if (isIntroductionQuestion(question)) {
            return evaluateIntroduction(
                    session,
                    question,
                    userAnswer
            );
        }

        return evaluate(
                session,
                question,
                userAnswer,
                "BEHAVIOURAL"
        );
    }


    // =========================================================
    // INTRODUCTION EVALUATION
    // =========================================================

    private String evaluateIntroduction(
            InterviewSession session,
            InterviewQuestion question,
            String userAnswer
    ) {

        return evaluate(
                session,
                question,
                userAnswer,
                "INTRODUCTION"
        );
    }


    // =========================================================
    // COMMON EVALUATION
    // =========================================================

    private String evaluate(
            InterviewSession session,
            InterviewQuestion question,
            String userAnswer,
            String type
    ) {

        String prompt = """
                You are a strict professional interviewer.

                ROLE:
                %s

                COMPANY:
                %s

                INTERVIEW TYPE:
                %s

                QUESTION:
                %s

                CANDIDATE ANSWER:
                %s

                JOB DESCRIPTION:
                %s

                Evaluate the candidate's answer.

                IMPORTANT:
                - Be strict and honest.
                - Relevance is extremely important.
                - If the answer is unrelated to the question,
                  relevance MUST be 0-2.
                - If the answer is unrelated, overall quality
                  must also be very low.
                - If the answer is nonsense or does not answer
                  the question, give negative feedback.
                - Do not invent candidate experience.
                - Do not give fake strengths.
                - Scores MUST be between 0 and 10.
                - Return ONLY valid JSON.
                - Do not use markdown.

                %s

                OUTPUT:

                {
                  "correctness": 0.0,
                  "completeness": 0.0,
                  "clarity": 0.0,
                  "depth": 0.0,
                  "relevance": 0.0,
                  "starSituation": 0.0,
                  "starTask": 0.0,
                  "starAction": 0.0,
                  "starResult": 0.0,
                  "strengths": "",
                  "missingConcepts": "",
                  "feedback": "",
                  "suggestedAnswer": ""
                }

                RULES:

                Technical:
                - correctness = factual/technical accuracy
                - completeness = coverage of required concepts
                - clarity = communication
                - depth = technical understanding
                - relevance = answer relevance

                Behavioural:
                - clarity = communication
                - relevance = relevance to question
                - starSituation = situation quality
                - starTask = task quality
                - starAction = candidate's personal action
                - starResult = outcome/result

                Introduction:
                - Do NOT use STAR.
                - Evaluate clarity and relevance.
                - Focus on professional background,
                  relevant skills, experience and career direction.

                Unrelated answer:
                - correctness = 0
                - completeness = 0
                - depth = 0
                - relevance = 0 or 1
                - score quality must be very low
                - strengths should say:
                  "No relevant strengths demonstrated."
                - feedback must clearly explain that
                  the answer did not address the question.

                suggestedAnswer:
                - Must directly answer the question.
                - Keep it below 100 words.
                - Never invent candidate experience.
                """
                .formatted(
                        limit(session.getJobRole(), 150),
                        limit(session.getCompanyName(), 150),
                        type,
                        limit(question.getQuestion(), 700),
                        limit(userAnswer, 2200),
                        limit(session.getJobDescription(), 1800),
                        typeInstructions(type)
                );

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }


    // =========================================================
    // TYPE INSTRUCTIONS
    // =========================================================

    private String typeInstructions(String type) {

        if ("TECHNICAL".equals(type)) {
            return """
                    This is a technical question.
                    Focus primarily on correctness,
                    completeness, depth and relevance.
                    """;
        }

        if ("INTRODUCTION".equals(type)) {
            return """
                    This is the opening "Tell me about yourself"
                    question.

                    Do NOT require STAR.
                    Evaluate whether the introduction is:
                    - professional
                    - concise
                    - structured
                    - role-relevant
                    - clear
                    """;
        }

        return """
                This is a behavioural question.
                Evaluate the candidate using STAR.
                Focus on the candidate's own actions,
                decisions and results.
                """;
    }


    // =========================================================
    // FOLLOW-UP QUESTION
    // =========================================================

    public String generateFollowUp(
            InterviewSession session,
            InterviewQuestion previousQuestion,
            String userAnswer
    ) {

        String prompt = """
                You are a professional interviewer.

                Role: %s
                Type: %s

                Previous question:
                %s

                Candidate answer:
                %s

                Generate ONE short follow-up question.

                The follow-up must:
                - relate directly to the answer
                - explore useful depth
                - remain relevant to the role
                - not repeat the question
                - return ONLY the question
                """
                .formatted(
                        limit(session.getJobRole(), 150),
                        previousQuestion.getQuestionType(),
                        limit(previousQuestion.getQuestion(), 700),
                        limit(userAnswer, 1800)
                );

        return cleanQuestion(
                chatClient
                        .prompt()
                        .user(prompt)
                        .call()
                        .content()
        );
    }


    // =========================================================
    // INTRODUCTION DETECTION
    // =========================================================

    private boolean isIntroductionQuestion(
            InterviewQuestion question
    ) {

        if (question == null) {
            return false;
        }

        if (question.getTopic() != null
                && question.getTopic()
                .equalsIgnoreCase("Introduction")) {
            return true;
        }

        String text = question.getQuestion();

        return text != null
                && text
                .toLowerCase()
                .contains("tell me about yourself");
    }


    // =========================================================
    // RAG
    // =========================================================

    public RagContext retrieveContext(
            InterviewSession session,
            String query
    ) {

        String safeQuery =
                query == null || query.isBlank()
                        ? safe(session.getJobRole())
                        : query;

        return new RagContext(
                retrieveJobDescription(
                        session,
                        safeQuery
                ),
                retrieveCompanyContext(
                        session,
                        safeQuery
                )
        );
    }


    private String retrieveJobDescription(
            InterviewSession session,
            String query
    ) {

        List<Document> documents =
                vectorStore.similaritySearch(
                        SearchRequest
                                .builder()
                                .query(query)
                                .topK(JD_TOP_K)
                                .filterExpression(
                                        "sessionId == '"
                                                + session.getId()
                                                + "'"
                                )
                                .build()
                );

        return joinDocuments(documents);
    }


    private String retrieveCompanyContext(
            InterviewSession session,
            String query
    ) {

        if (session.getCompanyKnowledge() == null) {
            return "";
        }

        Long id =
                session.getCompanyKnowledge().getId();

        List<Document> documents =
                vectorStore.similaritySearch(
                        SearchRequest
                                .builder()
                                .query(query)
                                .topK(COMPANY_TOP_K)
                                .filterExpression(
                                        "companyKnowledgeId == '"
                                                + id
                                                + "'"
                                )
                                .build()
                );

        return joinDocuments(documents);
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private String joinDocuments(
            List<Document> documents
    ) {

        if (documents == null || documents.isEmpty()) {
            return "";
        }

        return documents
                .stream()
                .map(Document::getText)
                .filter(
                        text ->
                                text != null
                                        && !text.isBlank()
                )
                .map(text -> limit(text, 700))
                .distinct()
                .collect(
                        Collectors.joining("\n")
                );
    }


    private String limit(
            String value,
            int max
    ) {

        if (value == null) {
            return "";
        }

        value = value.trim();

        return value.length() > max
                ? value.substring(0, max)
                : value;
    }


    private String safe(String value) {
        return value == null ? "" : value;
    }


    private String cleanQuestion(
            String response
    ) {

        if (response == null) {
            return "";
        }

        return response
                .replace("```text", "")
                .replace("```", "")
                .replace("\"", "")
                .trim();
    }


    // =========================================================
    // RAG CONTEXT
    // =========================================================

    public record RagContext(
            String jobDescription,
            String company
    ) {
    }
}
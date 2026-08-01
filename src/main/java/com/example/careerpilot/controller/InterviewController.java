package com.example.careerpilot.controller;

import com.example.careerpilot.dto.*;
import com.example.careerpilot.model.User;
import com.example.careerpilot.repo.UserRepo;
import com.example.careerpilot.service.InterviewService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    private final UserRepo userRepository;


    // ==========================================
    // START INTERVIEW
    // ==========================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewResponse startInterview(
            @RequestBody StartInterviewRequest request,
            Authentication authentication
    ) {

        User user =
                getCurrentUser(authentication);


        return interviewService
                .startInterview(
                        user,
                        request
                );
    }


    // ==========================================
    // GENERATE NEXT QUESTION
    // ==========================================

    @PostMapping("/{sessionId}/questions")
    public QuestionResponse generateQuestion(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {

        User user =
                getCurrentUser(authentication);


        return interviewService
                .generateQuestion(
                        sessionId,
                        user
                );
    }


    // ==========================================
    // SUBMIT ANSWER
    // ==========================================

    @PostMapping(
            "/{sessionId}/questions/{questionId}/answer"
    )
    public AnswerEvaluationResponse submitAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @RequestBody AnswerRequest request,
            Authentication authentication
    ) {

        User user =
                getCurrentUser(authentication);


        return interviewService
                .submitAnswer(
                        sessionId,
                        questionId,
                        request.getAnswer(),
                        user
                );
    }


    // ==========================================
    // FOLLOW-UP
    // ==========================================

    @PostMapping(
            "/{sessionId}/questions/{questionId}/follow-up"
    )
    public QuestionResponse generateFollowUp(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            Authentication authentication
    ) {

        User user =
                getCurrentUser(authentication);


        return interviewService
                .generateFollowUp(
                        sessionId,
                        questionId,
                        user
                );
    }


    // ==========================================
    // COMPLETE INTERVIEW
    // ==========================================

    @PostMapping("/{sessionId}/complete")
    public InterviewReportResponse completeInterview(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {

        User user =
                getCurrentUser(authentication);


        return interviewService
                .completeInterview(
                        sessionId,
                        user
                );
    }


    // ==========================================
    // HISTORY
    // ==========================================

    @GetMapping
    public List<InterviewResponse> getHistory(
            Authentication authentication
    ) {

        User user =
                getCurrentUser(authentication);


        return interviewService
                .getHistory(user);
    }


    // ==========================================
    // SINGLE INTERVIEW
    // ==========================================

    @GetMapping("/{sessionId}")
    public InterviewResponse getInterview(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {

        User user =
                getCurrentUser(authentication);


        return interviewService
                .getInterview(
                        sessionId,
                        user
                );
    }


    // ==========================================
    // INTERVIEW QUESTIONS
    // ==========================================

    @GetMapping("/{sessionId}/questions")
    public List<QuestionResponse> getQuestions(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {

        User user =
                getCurrentUser(authentication);


        return interviewService
                .getQuestions(
                        sessionId,
                        user
                );
    }


    // ==========================================
    // CURRENT USER
    // ==========================================

    private User getCurrentUser(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }


        String email =
                authentication.getName();


        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Authenticated user not found"
                                )
                );
    }
}
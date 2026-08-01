package com.example.careerpilot.controller;

import com.example.careerpilot.dto.CodingResponse;
import com.example.careerpilot.model.CodingQuestion;
import com.example.careerpilot.service.CodingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/coding")
public class CodingController {

    private final CodingService codingService;

    public CodingController(
            CodingService codingService) {

        this.codingService = codingService;
    }

    // ---------------------------------------------------------
    // CATALOG
    // ---------------------------------------------------------

    @GetMapping
    public ResponseEntity<CodingResponse> getCatalog(
            Authentication authentication) {

        return ResponseEntity.ok(
                codingService.getCatalog(
                        authentication
                )
        );
    }

    // ---------------------------------------------------------
    // TOPIC DETAILS
    // ---------------------------------------------------------

    @GetMapping("/topics/{topicId}")
    public ResponseEntity<CodingResponse.TopicDetails>
    getTopic(
            @PathVariable Long topicId,
            Authentication authentication) {

        return ResponseEntity.ok(
                codingService.getTopic(
                        topicId,
                        authentication
                )
        );
    }

    // ---------------------------------------------------------
    // SEARCH / FILTER
    // ---------------------------------------------------------

    @GetMapping("/questions")
    public ResponseEntity<CodingResponse.QuestionPage>
    getQuestions(

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            CodingQuestion.Difficulty difficulty,

            @RequestParam(required = false)
            CodingQuestion.Platform platform,

            @RequestParam(required = false)
            Boolean solved,

            @RequestParam(required = false)
            Long topicId,

            @RequestParam(required = false)
            String company,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            Authentication authentication) {

        return ResponseEntity.ok(
                codingService.getQuestions(
                        search,
                        difficulty,
                        platform,
                        solved,
                        topicId,
                        company,
                        page,
                        size,
                        authentication
                )
        );
    }

    // ---------------------------------------------------------
    // SOLVED CHECKBOX
    // ---------------------------------------------------------

    @PutMapping("/questions/{questionId}/solved")
    public ResponseEntity<Map<String, Boolean>>
    setSolved(

            @PathVariable Long questionId,

            @RequestParam boolean solved,

            Authentication authentication) {

        boolean value =
                codingService.setSolved(
                        questionId,
                        solved,
                        authentication
                );

        return ResponseEntity.ok(
                Map.of(
                        "solved",
                        value
                )
        );
    }

    // ---------------------------------------------------------
    // PROGRESS
    // ---------------------------------------------------------

    @GetMapping("/progress")
    public ResponseEntity<CodingResponse.Progress>
    getProgress(
            Authentication authentication) {

        return ResponseEntity.ok(
                codingService.getProgress(
                        authentication
                )
        );
    }
}
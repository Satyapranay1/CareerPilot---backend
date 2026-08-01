package com.example.careerpilot.controller;

import com.example.careerpilot.dto.ResumeAnalysisResponse;
import com.example.careerpilot.dto.ResumeHistoryResponse;
import com.example.careerpilot.service.ResumeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeAnalysisService resumeAnalysisService;

    // =========================================================
    // ANALYZE RESUME
    // =========================================================

    @PostMapping(
            value = "/analyze",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<ResumeAnalysisResponse> analyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam("company") String company,
            @RequestParam("role") String role
    ) {

        ResumeAnalysisResponse response =
                resumeAnalysisService.analyze(
                        file,
                        company,
                        role
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // HISTORY
    // =========================================================

    @GetMapping
    public ResponseEntity<List<ResumeHistoryResponse>> history() {

        return ResponseEntity.ok(
                resumeAnalysisService.getHistory()
        );
    }

    // =========================================================
    // ANALYSIS DETAILS
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<ResumeAnalysisResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                resumeAnalysisService.getById(id)
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        resumeAnalysisService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
package com.example.careerpilot.controller;

import com.example.careerpilot.dto.ResumeAnalysisDTO;
import com.example.careerpilot.dto.ResumeHistoryDTO;
import com.example.careerpilot.dto.ResumeResponse;
import com.example.careerpilot.model.User;
import com.example.careerpilot.repo.UserRepo;
import com.example.careerpilot.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final UserRepo userRepo;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> uploadResume(

            @RequestParam MultipartFile file,

            @RequestParam String company,

            @RequestParam String jobRole,

            @RequestParam(required = false) String jobDescription,

            Authentication authentication
    ) {

        User user = getCurrentUser(authentication);

        return ResponseEntity.ok(
                resumeService.uploadResume(
                        file,
                        user.getId(),
                        company,
                        jobRole,
                        jobDescription
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<ResumeHistoryDTO>> getHistory(
            Authentication authentication
    ) {

        User user = getCurrentUser(authentication);

        return ResponseEntity.ok(
                resumeService.getHistory(user.getId())
        );
    }

    private User getCurrentUser(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }

        String email = authentication.getName();

        return userRepo
                .findByEmail(email)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Authenticated user not found"
                        )
                );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeAnalysisDTO> getResume(

            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                resumeService.getResume(id)
        );
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> downloadResume(

            @PathVariable Long id
    ) throws Exception {

        return resumeService.downloadResume(id);
    }

    @PostMapping("/{id}/reanalyze")
    public ResponseEntity<ResumeResponse> reanalyzeResume(

            @PathVariable Long id,

            @RequestParam String company,

            @RequestParam String jobRole,

            @RequestParam(required = false) String jobDescription
    ) {

        return ResponseEntity.ok(
                resumeService.reanalyzeResume(
                        id,
                        company,
                        jobRole,
                        jobDescription
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(

            @PathVariable Long id
    ) {

        resumeService.deleteResume(id);

        return ResponseEntity.noContent().build();
    }

}
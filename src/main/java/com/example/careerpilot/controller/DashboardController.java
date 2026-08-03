package com.example.careerpilot.controller;

import com.example.careerpilot.dto.dashboard.DashboardResponse;
import com.example.careerpilot.model.User;
import com.example.careerpilot.repo.UserRepo;
import com.example.careerpilot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    private final UserRepo userRepo;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            Authentication authentication) {

        User user =
                userRepo.findByEmail(authentication.getName())
                        .orElseThrow(() ->
                                new RuntimeException("User not found"));

        return ResponseEntity.ok(
                dashboardService.getDashboard(user.getId())
        );
    }
}
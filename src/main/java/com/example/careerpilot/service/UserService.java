package com.example.careerpilot.service;

import com.example.careerpilot.model.User;
import com.example.careerpilot.repo.UserRepo;
import com.example.careerpilot.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final JwtUtils jwtUtils;
    private final UserRepo userRepo;
    public Optional<User> getUserFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }

        try {
            String token = header.substring(7); // remove "Bearer "
            Integer userId = Math.toIntExact(jwtUtils.extractUserId(token));
            return userRepo.findById(Long.valueOf(userId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

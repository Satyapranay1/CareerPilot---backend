package com.example.careerpilot.security;

import com.example.careerpilot.model.User;
import com.example.careerpilot.repo.UserRepo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserRepo userRepo;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // ==========================================
        // 1. GET AUTHORIZATION HEADER
        // ==========================================

        String authHeader =
                request.getHeader("Authorization");


        // ==========================================
        // 2. CHECK BEARER TOKEN
        // ==========================================

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        // ==========================================
        // 3. EXTRACT JWT
        // ==========================================

        String token =
                authHeader.substring(7);
        System.out.println(
                "JWT received: "
                        + !token.isBlank()
        );
        // ==========================================
        // 4. VALIDATE JWT
        // ==========================================

        if (!jwtUtils.isTokenValid(token)) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        // ==========================================
        // 5. CHECK EXISTING AUTHENTICATION
        // ==========================================

        if (SecurityContextHolder
                .getContext()
                .getAuthentication() != null) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        try {

            // ==========================================
            // 6. EXTRACT USER ID FROM JWT
            // ==========================================

            Long userId =
                    jwtUtils.extractUserId(token);


            // ==========================================
            // 7. FIND USER
            // ==========================================

            User user =
                    userRepo
                            .findById(userId)
                            .orElse(null);


            if (user != null) {

                // ==========================================
                // 8. CREATE USER DETAILS
                // ==========================================

                UserDetails userDetails =
                        org.springframework.security
                                .core
                                .userdetails
                                .User
                                .builder()

                                .username(
                                        user.getEmail()
                                )

                                .password(
                                        user.getPasswordHash()
                                )

                                .authorities(
                                        Collections.emptyList()
                                )

                                .build();


                // ==========================================
                // 9. CREATE AUTHENTICATION
                // ==========================================

                UsernamePasswordAuthenticationToken
                        authenticationToken =

                        new UsernamePasswordAuthenticationToken(

                                userDetails,

                                null,

                                userDetails
                                        .getAuthorities()
                        );


                authenticationToken.setDetails(

                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );


                // ==========================================
                // 10. STORE AUTHENTICATION
                // ==========================================

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                authenticationToken
                        );
            }

        } catch (Exception e) {
            System.err.println(
                    "JWT authentication failed: "
                            + e.getMessage()
            );

            e.printStackTrace();
            /*
             * Invalid token subject,
             * missing user, etc.
             *
             * Authentication remains empty.
             * Spring Security will reject protected
             * endpoints.
             */
        }


        // ==========================================
        // 11. CONTINUE REQUEST
        // ==========================================

        filterChain.doFilter(
                request,
                response
        );
    }
}
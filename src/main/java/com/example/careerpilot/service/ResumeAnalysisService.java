package com.example.careerpilot.service;

import com.example.careerpilot.model.User;
import com.example.careerpilot.repo.UserRepo;
import com.example.careerpilot.dto.*;
import com.example.careerpilot.model.ResumeAnalysis;
import com.example.careerpilot.exception.ResumeNotFoundException;
import com.example.careerpilot.parser.ResumeParser;
import com.example.careerpilot.rag.RoleExpectationRetriever;
import com.example.careerpilot.repo.ResumeAnalysisRepository;
import com.example.careerpilot.storage.FileStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeAnalysisService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;

    private final UserRepo userRepository;

    private final FileStorageService fileStorageService;

    private final ResumeParser resumeParser;

    private final RoleExpectationRetriever roleExpectationRetriever;

    private final KeywordMatcher keywordMatcher;

    private final ResumeQualityAnalyzer resumeQualityAnalyzer;

    private final AtsScoreCalculator atsScoreCalculator;

    private final ObjectMapper objectMapper;

    // =========================================================
    // ANALYZE
    // =========================================================

    @Transactional
    public ResumeAnalysisResponse analyze(
            MultipartFile file,
            String company,
            String role
    ) {

        validateRequest(file, company, role);

        User user = getCurrentUser();

        /*
         * Parse before storing.
         *
         * Invalid/scanned PDFs therefore don't create stored files.
         */
        String extractedText =
                resumeParser.parse(file);

        /*
         * Requirements must come from the existing knowledge base.
         * If context is insufficient, retrieval throws and the
         * analysis stops.
         */
        List<RoleExpectation> expectations =
                roleExpectationRetriever.retrieve(
                        company.trim(),
                        role.trim()
                );

        KeywordAnalysisResult keywordAnalysis =
                keywordMatcher.analyze(
                        extractedText,
                        expectations
                );

        QualityAnalysisResult qualityAnalysis =
                resumeQualityAnalyzer.analyze(
                        extractedText,
                        keywordAnalysis
                );

        /*
         * Final ATS Compatibility Score is calculated only here
         * using deterministic Java logic.
         */
        int atsScore =
                atsScoreCalculator.calculate(
                        keywordAnalysis.getKeywordMatchScore(),
                        qualityAnalysis.getImpactScore(),
                        qualityAnalysis.getReadabilityScore(),
                        qualityAnalysis.getGrammarScore(),
                        qualityAnalysis.getStructureScore()
                );

        String storedPath = null;

        try {

            /*
             * Only store the PDF after analysis has succeeded.
             */
            storedPath = fileStorageService.store(
                    file,
                    user.getId()
            );

            ResumeAnalysis entity =
                    buildEntity(
                            user,
                            file,
                            company,
                            role,
                            extractedText,
                            storedPath,
                            keywordAnalysis,
                            qualityAnalysis,
                            atsScore
                    );

            ResumeAnalysis saved =
                    resumeAnalysisRepository.saveAndFlush(entity);

            return toResponse(saved);

        } catch (RuntimeException exception) {

            /*
             * Filesystem operations are not part of the DB
             * transaction. If storage succeeded but persistence
             * failed, remove the newly created PDF.
             */
            if (storedPath != null) {
                safeDelete(storedPath);
            }

            throw exception;
        }
    }

    // =========================================================
    // HISTORY
    // =========================================================

    @Transactional(readOnly = true)
    public List<ResumeHistoryResponse> getHistory() {

        User user = getCurrentUser();

        return resumeAnalysisRepository
                .findAllByUserIdOrderByCreatedAtDesc(
                        user.getId()
                )
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    // =========================================================
    // DETAILS
    // =========================================================

    @Transactional(readOnly = true)
    public ResumeAnalysisResponse getById(Long id) {

        User user = getCurrentUser();

        ResumeAnalysis analysis =
                resumeAnalysisRepository
                        .findByIdAndUserId(
                                id,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ResumeNotFoundException(
                                        "Resume analysis not found."
                                )
                        );

        return toResponse(analysis);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Transactional
    public void delete(Long id) {

        User user = getCurrentUser();

        ResumeAnalysis analysis =
                resumeAnalysisRepository
                        .findByIdAndUserId(
                                id,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ResumeNotFoundException(
                                        "Resume analysis not found."
                                )
                        );

        String storedPath =
                analysis.getStoredFilePath();

        /*
         * Delete DB row first inside the transaction.
         */
        resumeAnalysisRepository.delete(analysis);

        resumeAnalysisRepository.flush();

        /*
         * Then delete the associated file.
         *
         * See note below regarding filesystem/DB transaction
         * boundaries.
         */
        fileStorageService.delete(storedPath);
    }

    // =========================================================
    // ENTITY
    // =========================================================

    private ResumeAnalysis buildEntity(
            User user,
            MultipartFile file,
            String company,
            String role,
            String extractedText,
            String storedPath,
            KeywordAnalysisResult keywordAnalysis,
            QualityAnalysisResult qualityAnalysis,
            int atsScore
    ) {

        ResumeAnalysis entity =
                new ResumeAnalysis();

        entity.setUser(user);

        entity.setCompany(company.trim());

        entity.setRole(role.trim());

        entity.setOriginalFilename(
                safeOriginalFilename(file)
        );

        entity.setStoredFilePath(storedPath);

        entity.setExtractedText(extractedText);

        entity.setAtsScore(atsScore);

        entity.setKeywordMatchScore(
                keywordAnalysis.getKeywordMatchScore()
        );

        entity.setImpactScore(
                qualityAnalysis.getImpactScore()
        );

        entity.setReadabilityScore(
                qualityAnalysis.getReadabilityScore()
        );

        entity.setGrammarScore(
                qualityAnalysis.getGrammarScore()
        );

        entity.setStructureScore(
                qualityAnalysis.getStructureScore()
        );

        entity.setMatchedKeywords(
                toJson(keywordAnalysis.getMatchedKeywords())
        );

        entity.setMissingKeywords(
                toJson(keywordAnalysis.getMissingKeywords())
        );

        entity.setWeakKeywords(
                toJson(keywordAnalysis.getWeakKeywords())
        );

        entity.setStrongAreas(
                toJson(qualityAnalysis.getStrongAreas())
        );

        entity.setSuggestions(
                toJson(qualityAnalysis.getSuggestions())
        );

        return entity;
    }

    // =========================================================
    // RESPONSE MAPPING
    // =========================================================

    private ResumeAnalysisResponse toResponse(
            ResumeAnalysis analysis
    ) {

        ResumeScores scores =
                new ResumeScores(
                        analysis.getKeywordMatchScore(),
                        analysis.getImpactScore(),
                        analysis.getReadabilityScore(),
                        analysis.getGrammarScore(),
                        analysis.getStructureScore()
                );

        return new ResumeAnalysisResponse(
                analysis.getId(),
                analysis.getCompany(),
                analysis.getRole(),
                analysis.getOriginalFilename(),
                analysis.getAtsScore(),
                scores,
                fromJson(analysis.getMatchedKeywords()),
                fromJson(analysis.getMissingKeywords()),
                fromJson(analysis.getWeakKeywords()),
                fromJson(analysis.getStrongAreas()),
                fromJson(analysis.getSuggestions()),
                analysis.getCreatedAt()
        );
    }

    private ResumeHistoryResponse toHistoryResponse(
            ResumeAnalysis analysis
    ) {

        return new ResumeHistoryResponse(
                analysis.getId(),
                analysis.getCompany(),
                analysis.getRole(),
                analysis.getOriginalFilename(),
                analysis.getAtsScore(),
                analysis.getCreatedAt()
        );
    }

    // =========================================================
    // AUTHENTICATED USER
    // =========================================================

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                authentication.getPrincipal()
        )) {

            throw new IllegalStateException(
                    "Authenticated user is required."
            );
        }

        String username =
                authentication.getName();

        /*
         * CAREERPILOT INTEGRATION POINT
         *
         * Use the same lookup used by your existing authenticated
         * Profile/Interview modules.
         *
         * If authentication.getName() contains the user's email:
         *
         *     findByEmail(username)
         *
         * If it contains username:
         *
         *     findByUsername(username)
         */

        return userRepository
                .findByEmail(username)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user could not be found."
                        )
                );
    }

    // =========================================================
    // JSONB
    // =========================================================

    private String toJson(List<String> values) {

        try {

            return objectMapper.writeValueAsString(
                    values == null
                            ? List.of()
                            : values
            );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Unable to serialize resume analysis.",
                    exception
            );
        }
    }

    private List<String> fromJson(String json) {

        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        try {

            return objectMapper.readValue(
                    json,
                    new TypeReference<List<String>>() {
                    }
            );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Unable to read stored resume analysis.",
                    exception
            );
        }
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateRequest(
            MultipartFile file,
            String company,
            String role
    ) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Resume PDF is required."
            );
        }

        if (company == null || company.isBlank()) {
            throw new IllegalArgumentException(
                    "Company is required."
            );
        }

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                    "Role is required."
            );
        }

        if (company.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Company must not exceed 150 characters."
            );
        }

        if (role.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Role must not exceed 150 characters."
            );
        }
    }

    private String safeOriginalFilename(
            MultipartFile file
    ) {

        String originalFilename =
                file.getOriginalFilename();

        if (originalFilename == null
                || originalFilename.isBlank()) {

            return "resume.pdf";
        }

        /*
         * Metadata only, but still remove path components.
         *
         * C:\fakepath\resume.pdf -> resume.pdf
         * ../../resume.pdf       -> resume.pdf
         */
        String normalized =
                originalFilename.replace('\\', '/');

        int lastSlash =
                normalized.lastIndexOf('/');

        if (lastSlash >= 0) {
            normalized =
                    normalized.substring(lastSlash + 1);
        }

        normalized = normalized.trim();

        if (normalized.isBlank()) {
            return "resume.pdf";
        }

        if (normalized.length() > 255) {
            normalized =
                    normalized.substring(
                            normalized.length() - 255
                    );
        }

        return normalized;
    }

    private void safeDelete(String storedPath) {

        try {
            fileStorageService.delete(storedPath);
        } catch (RuntimeException ignored) {
            /*
             * Preserve the original analysis/database exception.
             * In production this should be logged.
             */
        }
    }
}
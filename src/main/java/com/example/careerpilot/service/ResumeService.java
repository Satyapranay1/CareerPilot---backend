package com.example.careerpilot.service;

import com.example.careerpilot.dto.KnowledgeContext;
import com.example.careerpilot.dto.ResumeAnalysisDTO;
import com.example.careerpilot.dto.ResumeHistoryDTO;
import com.example.careerpilot.dto.ResumeResponse;
import com.example.careerpilot.model.Resume;
import com.example.careerpilot.repo.ResumeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final PdfParser pdfParser;
    private final KnowledgeService knowledgeService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private static final int MAX_RESUME_CHARS = 12000;
    private static final int MAX_KNOWLEDGE_CHARS = 6000;
    private static final int MAX_JOB_DESCRIPTION_CHARS = 5000;

    @Value("${resume.upload-path:uploads/resumes}")
    private String uploadPath;

    public ResumeResponse uploadResume(
            MultipartFile file,
            Long userId,
            String company,
            String jobRole,
            String jobDescription) {

        try {

            log.info("Starting resume analysis");

            // Save uploaded PDF
            String filePath = saveFile(file);

            // Extract text
            String resumeText = pdfParser.extractText(file);

            log.info("Resume parsed successfully");

            // Retrieve Knowledge
            KnowledgeContext knowledge =
                    knowledgeService.getKnowledge(
                            company,
                            jobRole,
                            jobDescription
                    );

            // Load Prompt
            String prompt = buildPrompt(
                    knowledge,
                    jobDescription
            );

            log.info("=========== FINAL PROMPT ===========");
            log.info(prompt);
            log.info("===================================");

            log.info("Calling Llama");

            // Call LLM
            String aiResponse = chatClient
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            aiResponse = cleanJson(aiResponse);

            log.info("========== LLM RESPONSE ==========");
            log.info(aiResponse);
            log.info("==================================");

            ResumeResponse response =
                    objectMapper.readValue(
                            aiResponse,
                            ResumeResponse.class
                    );

            response.setKnowledgeSource(
                    knowledge.source()
            );

            if (response.getSummary() == null) {
                response.setSummary("");
            }

            double ats = calculateATS(response);

            ats = Math.round(ats * 100.0) / 100.0;

            response.setAtsScore(ats);

            JsonNode analysisJson = objectMapper.valueToTree(aiResponse);

            saveResume(
                    userId,
                    file.getOriginalFilename(),
                    filePath,
                    company,
                    jobRole,
                    resumeText,
                    analysisJson,
                    ats
            );

            log.info("Resume analysis completed");

            return response;

        } catch (Exception ex) {

            log.error("Resume analysis failed", ex);

            throw new RuntimeException(
                    "Unable to analyze resume",
                    ex
            );
        }
    }

    public ResumeAnalysisDTO getResume(Long id) {

        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Resume not found"));

        try {

            JsonNode analysisJson =
                    resume.getAnalysisJson();

            if (analysisJson == null ||
                    analysisJson.isNull()) {

                throw new RuntimeException(
                        "Resume analysis is empty"
                );
            }

            ResumeResponse analysis;

            if (analysisJson.isTextual()) {

                analysis = objectMapper.readValue(
                        analysisJson.asText(),
                        ResumeResponse.class
                );

            } else {

                analysis = objectMapper.treeToValue(
                        analysisJson,
                        ResumeResponse.class
                );
            }

            return ResumeAnalysisDTO.builder()
                    .id(resume.getId())
                    .fileName(resume.getFileName())
                    .company(resume.getCompany())
                    .jobRole(resume.getJobRole())
                    .knowledgeSource(
                            analysis.getKnowledgeSource()
                    )
                    .atsScore(resume.getAtsScore())
                    .uploadedAt(resume.getCreatedAt())
                    .analysis(analysis)
                    .build();

        } catch (Exception e) {

            log.error(
                    "Unable to parse analysis for resume {}",
                    id,
                    e
            );

            throw new RuntimeException(
                    "Unable to parse resume analysis",
                    e
            );
        }
    }

    private String buildPrompt(
            KnowledgeContext knowledge,
            String jobDescription
    ) {

        String companyKnowledge = limitText(
                knowledge.content(),
                MAX_KNOWLEDGE_CHARS
        );

        String limitedJobDescription = limitText(
                jobDescription,
                MAX_JOB_DESCRIPTION_CHARS
        );

        return """
            You are an expert ATS resume reviewer and technical recruiter.

            Analyze the candidate resume against the TARGET ROLE and JOB DESCRIPTION.

            IMPORTANT:
            - Use only information supported by the resume and provided context.
            - Never invent skills, experience, projects, companies, education, or achievements.
            - Evaluate the candidate specifically for the target role.
            - Do not treat unrelated technologies as role-relevant.
            - Keep responses concise.
            - Return ONLY valid JSON.
            - Do not use markdown.
            - Do not add text outside JSON.

            KNOWLEDGE SOURCE:
            %s

            RELEVANT KNOWLEDGE:
            %s

            JOB DESCRIPTION:
            %s

            Return JSON matching this EXACT structure:

            {
              "knowledgeSource": "",
              "summary": "",
              "atsScore": 0,

              "scores": {
                "keywordMatch": 0,
                "impact": 0,
                "readability": 0,
                "grammar": 0,
                "structure": 0
              },

              "strongAreas": [],
              "weakAreas": [],
              "missingKeywords": [],
              "missingSkills": [],
              "improvementSuggestions": [],

              "roleRelevantSkills": [],
              "missingRoleSkills": [],
              "roleSpecificInsights": [],

              "roleFit": {
                "score": 0,
                "level": "",
                "explanation": ""
              },

              "technologies": {
                "programmingLanguages": [],
                "frameworks": [],
                "databases": [],
                "cloud": [],
                "devOps": [],
                "testing": [],
                "tools": [],
                "other": []
              },

              "skillMatch": [],
              "skillGaps": [],
              "skillCategories": [],

              "projectAnalysis": [],
              "experienceAnalysis": [],
              "achievementAnalysis": [],

              "atsAnalysis": {
                "strengths": [],
                "weaknesses": [],
                "recommendations": [],
                "keywordCoverage": 0
              },

              "sectionAnalysis": [],
              "bulletAnalysis": [],

              "careerLevel": {
                "level": "",
                "confidence": 0,
                "explanation": ""
              },

              "recruiterImpression": {
                "firstImpression": "",
                "positives": [],
                "concerns": [],
                "hiringLikelihood": 0
              },

              "interviewReadiness": {
                "score": 0,
                "strengths": [],
                "technicalTopics": [],
                "projectTopics": [],
                "behavioralTopics": [],
                "preparationSuggestions": []
              },

              "redFlags": [],
              "priorityMatrix": [],

              "actionPlan": {
                "immediate": [],
                "shortTerm": [],
                "mediumTerm": []
              }
            }

            RULES:
            - All scores must be between 0 and 100.
            - atsScore = overall ATS compatibility.
            - keywordMatch = target-role keyword coverage.
            - roleFit.score = suitability for the target role.
            - Keep arrays concise, normally 3-5 items.
            - missingKeywords and missingSkills may contain up to 8 items.
            - Keep each text item short and actionable.
            - Do not invent missing information.

            ROLE-SPECIFIC EVALUATION:
            Prioritize skills required by the target role.

            Example:
            Data Scientist → Python, SQL, statistics, machine learning,
            pandas, NumPy, scikit-learn, TensorFlow, data analysis.

            Java Backend Developer → Java, Spring Boot, REST APIs,
            PostgreSQL, JPA, microservices.

            Frontend Developer → JavaScript, TypeScript, React,
            HTML, CSS, UI development.

            Do not give role-relevant credit to unrelated technologies
            simply because they exist in the resume.
            IMPORTANT JSON TYPE RULES:
        
            - skillMatch MUST contain objects with:
              skill, score, evidence
        
            - skillGaps MUST contain objects with:
              skill, importance, reason, recommendation
        
            - skillCategories MUST contain objects with:
              category, score, matchedSkills, missingSkills
        
            - projectAnalysis MUST contain objects with:
              project, relevance, strengths, weaknesses, missingDetails, recommendations
        
            - experienceAnalysis MUST contain objects with:
              company, role, relevance, strengths, weaknesses, recommendations
        
            - achievementAnalysis MUST contain objects with:
              achievement, impact, strengths, recommendations
        
            - sectionAnalysis MUST contain objects with:
              section, score, strengths, weaknesses, recommendations
        
            - bulletAnalysis MUST contain objects with:
              original, improved, issue, impact
        
            - priorityMatrix MUST contain objects with:
              item, priority, impact, effort, recommendation
        
            - All other arrays must contain strings unless the JSON structure explicitly shows objects.
        
            - NEVER replace an object with a string.
            - NEVER replace an array of objects with an array of strings.
            """.formatted(
                safe(knowledge.source()),
                companyKnowledge,
                limitedJobDescription
        );
    }

    private String limitText(String text, int maxChars) {

        if (text == null || text.isBlank()) {
            return "";
        }

        String cleaned = text.trim();

        if (cleaned.length() <= maxChars) {
            return cleaned;
        }

        return cleaned.substring(0, maxChars)
                + "\n[Content truncated for performance]";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private double calculateATS(
            ResumeResponse response) {

        ResumeResponse.Scores score =
                response.getScores();

        return score.getKeywordMatch() * 0.35
                + score.getImpact() * 0.25
                + score.getReadability() * 0.20
                + score.getGrammar() * 0.10
                + score.getStructure() * 0.10;
    }

    private void saveResume(
            Long userId,
            String fileName,
            String filePath,
            String company,
            String jobRole,
            String resumeText,
            JsonNode analysisJson,
            double ats) {

        Resume resume = Resume.builder()
                .userId(userId)
                .fileName(fileName)
                .filePath(filePath)
                .company(company)
                .jobRole(jobRole)
                .resumeText(resumeText)
                .analysisJson(analysisJson)
                .atsScore(BigDecimal.valueOf(ats))
                .build();

        resumeRepository.save(resume);
    }

    private String saveFile(MultipartFile file) throws IOException {

        Path uploadDirectory = Path.of(uploadPath);

        if (Files.notExists(uploadDirectory)) {
            Files.createDirectories(uploadDirectory);
        }

        String fileName = UUID.randomUUID()
                + "_"
                + file.getOriginalFilename();

        Path destination = uploadDirectory.resolve(fileName);

        Files.copy(
                file.getInputStream(),
                destination,
                StandardCopyOption.REPLACE_EXISTING
        );

        return destination.toString();
    }

    private String cleanJson(String response) {

        if (response == null || response.isBlank()) {
            throw new RuntimeException("AI returned an empty response");
        }

        String cleaned = response.trim();

        // Remove markdown code fences
        cleaned = cleaned
                .replaceAll("(?i)```json", "")
                .replaceAll("(?i)```", "")
                .trim();

        // Find the first JSON object
        int start = cleaned.indexOf('{');

        if (start == -1) {
            log.error("AI response does not contain JSON: {}", cleaned);
            throw new RuntimeException(
                    "AI did not return valid JSON"
            );
        }

        /*
         * Find the matching closing brace instead of simply
         * using lastIndexOf("}").
         *
         * This handles cases where the AI adds text before
         * or after the JSON.
         */
        int depth = 0;
        boolean insideString = false;
        boolean escaped = false;
        int end = -1;

        for (int i = start; i < cleaned.length(); i++) {

            char c = cleaned.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && insideString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                insideString = !insideString;
                continue;
            }

            if (insideString) {
                continue;
            }

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;

                if (depth == 0) {
                    end = i;
                    break;
                }
            }
        }

        if (end == -1) {
            log.error("Invalid/incomplete JSON from AI: {}", cleaned);

            throw new RuntimeException(
                    "AI returned incomplete JSON"
            );
        }

        String json = cleaned.substring(start, end + 1).trim();

        // Validate JSON before returning it
        try {

            objectMapper.readTree(json);

            return json;

        } catch (Exception e) {

            log.error(
                    "AI returned invalid JSON: {}",
                    json
            );

            throw new RuntimeException(
                    "AI returned invalid JSON",
                    e
            );
        }
    }

    public void deleteResume(Long id) {

        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Resume not found"));

        try {

            if (resume.getFilePath() != null &&
                    !resume.getFilePath().isBlank()) {

                Files.deleteIfExists(
                        Path.of(resume.getFilePath())
                );
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to delete resume file",
                    e
            );
        }

        resumeRepository.delete(resume);

        log.info("Resume {} deleted successfully", id);
    }

    public ResumeResponse reanalyzeResume(
            Long id,
            String company,
            String jobRole,
            String jobDescription) {

        long totalStart = System.currentTimeMillis();

        try {

            Resume resume = resumeRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("Resume not found"));

            long knowledgeStart = System.currentTimeMillis();

            KnowledgeContext knowledge =
                    knowledgeService.getKnowledge(
                            company,
                            jobRole,
                            limitText(
                                    jobDescription,
                                    MAX_JOB_DESCRIPTION_CHARS
                            )
                    );

            log.info(
                    "Resume knowledge retrieval: {} ms",
                    System.currentTimeMillis() - knowledgeStart
            );

            String prompt = buildPrompt(
                    knowledge,
                    resume.getResumeText()
            );

            log.info(
                    "Resume prompt size: {} characters",
                    prompt.length()
            );

            long aiStart = System.currentTimeMillis();

            String aiResponse = chatClient
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info(
                    "Resume AI generation: {} ms",
                    System.currentTimeMillis() - aiStart
            );

            aiResponse = cleanJson(aiResponse);

            ResumeResponse response =
                    objectMapper.readValue(
                            aiResponse,
                            ResumeResponse.class
                    );

            response.setKnowledgeSource(
                    knowledge.source()
            );

            if (response.getSummary() == null) {
                response.setSummary("");
            }

            double ats = calculateATS(response);

            ats = Math.round(ats * 100.0) / 100.0;

            response.setAtsScore(ats);

            JsonNode analysisJson =
                    objectMapper.readTree(aiResponse);

            resume.setCompany(company);
            resume.setJobRole(jobRole);
            resume.setAnalysisJson(analysisJson);
            resume.setAtsScore(
                    BigDecimal.valueOf(ats)
            );

            resumeRepository.save(resume);

            log.info(
                    "Total resume analysis time: {} ms",
                    System.currentTimeMillis() - totalStart
            );

            return response;

        } catch (Exception ex) {

            log.error(
                    "Resume reanalysis failed",
                    ex
            );

            throw new RuntimeException(
                    "Unable to reanalyze resume",
                    ex
            );
        }
    }

    public ResponseEntity<byte[]> downloadResume(Long id) {

        try {

            Resume resume = resumeRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("Resume not found"));

            Path path = Path.of(resume.getFilePath());

            if (Files.notExists(path)) {
                throw new RuntimeException("Resume file not found");
            }

            byte[] file = Files.readAllBytes(path);

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" +
                                    resume.getFileName() + "\""
                    )
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(file.length)
                    .body(file);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to download resume",
                    e
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ResumeHistoryDTO> getHistory(Long userId) {

        List<Resume> resumes =
                resumeRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return resumes.stream()
                .map(resume -> ResumeHistoryDTO.builder()
                        .id(resume.getId())
                        .fileName(resume.getFileName())
                        .company(resume.getCompany())
                        .jobRole(resume.getJobRole())
                        .atsScore(resume.getAtsScore())
                        .uploadedAt(resume.getCreatedAt())
                        .build())
                .toList();
    }
}
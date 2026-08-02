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
                    resumeText
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

            ResumeResponse analysis = objectMapper.treeToValue(
                    resume.getAnalysisJson(),
                    ResumeResponse.class
            );

            return ResumeAnalysisDTO.builder()
                    .id(resume.getId())
                    .fileName(resume.getFileName())
                    .company(resume.getCompany())
                    .jobRole(resume.getJobRole())
                    .knowledgeSource(analysis.getKnowledgeSource())
                    .atsScore(resume.getAtsScore())
                    .uploadedAt(resume.getCreatedAt())
                    .analysis(analysis)
                    .build();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to parse resume analysis",
                    e
            );
        }
    }

    private String buildPrompt(
            KnowledgeContext knowledge,
            String resumeText) {

        return """
You are an expert Technical Recruiter, ATS Resume Expert, Hiring Manager, Career Coach, and Senior Software Engineer with extensive experience hiring software engineers for enterprise technology companies.

Your task is to perform an objective, evidence-based evaluation of a candidate's resume.

=========================================================================
KNOWLEDGE SOURCE
=========================================================================

Knowledge Source:
%s

Knowledge Context:
%s

=========================================================================
RESUME
=========================================================================

%s

=========================================================================
EVALUATION RULES
=========================================================================

First determine the evaluation context.

If Knowledge Source = RAG

• Use ONLY the supplied Knowledge Context as the hiring requirements.
• Do not make assumptions beyond the retrieved knowledge.

If Knowledge Source = JOB_DESCRIPTION

• Treat the supplied Knowledge Context as the official Job Description.
• Evaluate the resume against it.

If Knowledge Source = GENERIC

• No company-specific hiring information exists.
• Evaluate the resume using modern software engineering hiring best practices.
• Clearly mention in the summary that this is a generic evaluation.

Never invent skills, projects, achievements, certifications, responsibilities, or experience.

Every observation must be directly supported by:

• the resume
or
• the supplied hiring knowledge.

If evidence is unavailable, do not assume.

=========================================================================
SCORING
=========================================================================

Return integer scores between 0 and 100.

Evaluate ONLY:

1. Keyword Match

Evaluate alignment for:

• Programming Languages
• Frameworks
• Databases
• Cloud
• DevOps
• APIs
• Testing
• Security
• Architecture
• Tools
• Soft Skills

2. Impact

Evaluate:

• Quantified achievements
• Metrics
• Ownership
• Scale
• Leadership
• Business impact

3. Readability

Evaluate:

• Formatting
• Section ordering
• White space
• Bullet consistency
• Resume flow

4. Grammar

Evaluate:

• Grammar
• Professional language
• Spelling
• Sentence clarity

5. Structure

Evaluate:

• Overall organization
• Experience section
• Projects
• Skills
• Education
• Certifications

DO NOT calculate ATS Score.

The backend calculates ATS separately.

=========================================================================
FIELD RULES
=========================================================================

SUMMARY

Write 3-5 professional sentences.

Include:

• Overall resume quality
• Alignment with the role
• Biggest strengths
• Biggest weaknesses
• Overall hiring impression

Never leave summary empty.

Never exaggerate candidate experience.

Never call someone "highly experienced" unless supported.

=========================================================================
STRONG AREAS
=========================================================================

Return 3-8 strengths.

Each strength MUST be directly supported by the resume.

Good examples

Java

Spring Boot

REST APIs

PostgreSQL

Problem Solving

System Design

Git

Team Collaboration

Bad examples

Excellent Developer

Great Team Player

Highly Skilled

=========================================================================
WEAK AREAS
=========================================================================

Return 3-8 broad improvement areas.

Weak Areas describe capability gaps.

Examples

Limited Cloud Experience

Limited Production Experience

Few Quantified Achievements

Limited Testing Experience

Weak Project Descriptions

Do NOT repeat Missing Keywords.

Weak Areas Rules
----------------
Weak Areas represent broad capability gaps.

Do NOT return technology names.

Good examples:
- Limited Cloud Experience
- Limited Testing Experience
- Few Quantified Achievements
- Limited Production Deployment Experience

Bad examples:
- Docker
- Kafka
- Kubernetes

Strong Areas Rules
------------------
Only include skills explicitly supported by the resume.
Do not infer or invent expertise.

Scoring Rules
-------------
Every score must be justified by the resume.
Do not use default values such as 70, 80, or 90 unless the evidence supports them.

Improvement Suggestions
-----------------------
Order suggestions from highest impact to lowest impact.

=========================================================================
MISSING KEYWORDS
=========================================================================

Return 5-15 exact ATS keywords missing from the resume.

Examples

Docker

Kafka

Redis

JUnit

Mockito

Kubernetes

CI/CD

AWS

Spring Security

JWT

These should be exact keywords.

=========================================================================
MISSING SKILLS
=========================================================================

Return 3-10 broader missing skills.

Examples

Cloud Deployment

Container Orchestration

Distributed Systems

Message Queues

Monitoring

Performance Optimization

Testing Strategy

Security Best Practices

Do not duplicate Missing Keywords.

=========================================================================
IMPROVEMENT SUGGESTIONS
=========================================================================

Return 5-10 highly actionable suggestions.

Each suggestion should tell the candidate exactly what to improve.

Good examples

Add quantified achievements to every project.

Mention API response time improvements.

Describe project scale.

Include Docker deployment.

Highlight PostgreSQL optimization.

Mention JUnit and Mockito testing.

Add CI/CD experience if applicable.

Expand project responsibilities.

Bad examples

Improve resume.

Write better.

Gain more experience.

=========================================================================
CONSISTENCY RULES
=========================================================================

Never contradict yourself.

Do not duplicate values across arrays.

Do not invent technologies.

Do not invent projects.

Do not invent achievements.

Do not invent certifications.

Do not leave arrays null.

Return empty arrays when necessary.

=========================================================================
OUTPUT
=========================================================================

Return ONLY valid JSON.

Do not explain.

Do not use markdown.

Do not wrap inside ```json.

The first character MUST be {

The last character MUST be }

{
  "summary": "",
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
  "improvementSuggestions": []
}
""".formatted(
                knowledge.source(),
                knowledge.content(),
                resumeText
        );
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
            return "";
        }

        response = response
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return response;
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

        try {

            Resume resume = resumeRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("Resume not found"));

            KnowledgeContext knowledge =
                    knowledgeService.getKnowledge(
                            company,
                            jobRole,
                            jobDescription
                    );

            String prompt = buildPrompt(
                    knowledge,
                    resume.getResumeText()
            );

            log.info("Reanalyzing resume {}", id);

            String aiResponse = chatClient
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            aiResponse = cleanJson(aiResponse);

            ResumeResponse response = objectMapper.readValue(
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
                    objectMapper.valueToTree(aiResponse);

            resume.setCompany(company);
            resume.setJobRole(jobRole);
            resume.setAnalysisJson(analysisJson);
            resume.setAtsScore(BigDecimal.valueOf(ats));

            resumeRepository.save(resume);

            log.info("Resume {} reanalyzed successfully", id);

            return response;

        } catch (Exception ex) {

            log.error("Resume reanalysis failed", ex);

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
package com.example.careerpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {

    private String knowledgeSource;

    private String summary;

    private Double atsScore;

    private Scores scores;

    private RoleFit roleFit;

    private List<String> strongAreas;

    private List<String> weakAreas;

    private Technologies technologies;

    private List<SkillMatch> skillMatch;

    private List<SkillGap> skillGaps;

    private List<String> missingKeywords;

    private List<String> missingSkills;

    private List<SkillCategory> skillCategories;

    private List<ProjectAnalysis> projectAnalysis;

    private List<ExperienceAnalysis> experienceAnalysis;

    private List<AchievementAnalysis> achievementAnalysis;

    private AtsAnalysis atsAnalysis;

    private List<SectionAnalysis> sectionAnalysis;

    private List<BulletAnalysis> bulletAnalysis;

    private CareerLevel careerLevel;

    private RecruiterImpression recruiterImpression;

    private InterviewReadiness interviewReadiness;

    private ActionPlan actionPlan;

    private List<String> improvementSuggestions;

    private List<String> redFlags;

    private List<PriorityItem> priorityMatrix;

    /*
     * Existing fields kept for backward compatibility
     */
    private List<String> roleRelevantSkills;

    private List<String> missingRoleSkills;

    private List<String> roleSpecificInsights;


    // =========================================================
    // SCORES
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Scores {

        private Double keywordMatch;

        private Double impact;

        private Double readability;

        private Double grammar;

        private Double structure;
    }


    // =========================================================
    // ROLE FIT
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleFit {

        private Double overallScore;

        private Double technicalFit;

        private Double experienceFit;

        private Double projectFit;

        private Double skillFit;

        private Double atsAlignment;

        private String overallAssessment;
    }


    // =========================================================
    // TECHNOLOGIES
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Technologies {

        private List<Technology> demonstrated;

        private List<Technology> partial;

        private List<Technology> missing;

        private List<Technology> recommended;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Technology {

        private String technology;

        private String category;

        private String status;

        private String importance;

        private String reason;
    }


    // =========================================================
    // SKILL MATCH
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillMatch {

        private String skill;

        private String evidence;

        private String status;

        private String importance;

        private String recommendation;
    }


    // =========================================================
    // SKILL GAPS
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillGap {

        private String skill;

        private String importance;

        private String whatIsMissing;

        private String whyItMatters;

        private String howToImprove;

        private String resumeAction;
    }


    // =========================================================
    // SKILL CATEGORIES
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillCategory {

        private String category;

        private Double score;

        private List<String> strengths;

        private List<String> gaps;
    }


    // =========================================================
    // PROJECT ANALYSIS
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectAnalysis {

        private String projectName;

        private List<String> technologies;

        private Double roleRelevance;

        private Double technicalDepth;

        private Double impactScore;

        private List<String> strengths;

        private List<String> weaknesses;

        private List<String> missingDetails;

        private List<String> recommendations;
    }


    // =========================================================
    // EXPERIENCE ANALYSIS
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperienceAnalysis {

        private String role;

        private String company;

        private Double roleRelevance;

        private Double technicalDepth;

        private Double ownershipScore;

        private Double impactScore;

        private List<String> strengths;

        private List<String> weaknesses;

        private List<String> missingMetrics;

        private List<String> recommendations;
    }


    // =========================================================
    // ACHIEVEMENT ANALYSIS
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AchievementAnalysis {

        private String achievement;

        private Boolean quantified;

        private Boolean technical;

        private Boolean measurable;

        private String assessment;

        private String recommendation;
    }


    // =========================================================
    // ATS ANALYSIS
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AtsAnalysis {

        private List<String> strengths;

        private List<String> risks;

        private List<String> improvements;
    }


    // =========================================================
    // SECTION ANALYSIS
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionAnalysis {

        private String section;

        private Double score;

        private List<String> strengths;

        private List<String> weaknesses;

        private List<String> recommendations;
    }


    // =========================================================
    // BULLET ANALYSIS
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulletAnalysis {

        private String section;

        private String issue;

        private String problem;

        private String recommendation;

        private String exampleStructure;
    }


    // =========================================================
    // CAREER LEVEL
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CareerLevel {

        private String level;

        private String reason;
    }


    // =========================================================
    // RECRUITER IMPRESSION
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecruiterImpression {

        private String firstImpression;

        private String topPositiveSignal;

        private String topConcern;

        private String biggestRejectionRisk;

        private String interviewLikelihood;
    }


    // =========================================================
    // INTERVIEW READINESS
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewReadiness {

        private List<String> technicalTopics;

        private List<String> projectTopics;

        private List<String> behavioralTopics;

        private List<String> resumeQuestions;
    }


    // =========================================================
    // ACTION PLAN
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionPlan {

        private List<String> immediate;

        private List<String> shortTerm;

        private List<String> mediumTerm;
    }


    // =========================================================
    // PRIORITY MATRIX
    // =========================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriorityItem {

        private Integer priority;

        private String issue;

        private String action;

        private String impact;
    }
}
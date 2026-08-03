package com.example.careerpilot.service.impl;

import com.example.careerpilot.dto.dashboard.*;
import com.example.careerpilot.model.*;
import com.example.careerpilot.repo.*;
import com.example.careerpilot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepo userRepo;

    private final ResumeRepository resumeRepository;

    private final InterviewSessionRepository interviewSessionRepository;

    private final UserQuestionProgressRepository progressRepository;

    private final CodingQuestionRepository codingQuestionRepository;

    private final CodingTopicRepository codingTopicRepository;

    private final SkillRepo skillRepository;

    @Override
    public DashboardResponse getDashboard(Long userId) {

        return DashboardResponse.builder()
                .hero(buildHero(userId))
                .metrics(buildMetrics(userId))
                .readinessTrend(buildInterviewTrend(userId))
                .weeklyActivity(buildWeeklyActivity(userId))
                .topicDistribution(buildTopicDistribution(userId))
                .skillRadar(buildSkillRadar(userId))
//                .leaderboard(buildLeaderboard(userId))
                .activities(buildActivities(userId))
                .upcomingTasks(buildUpcomingTasks(userId))
                .build();
    }

    // ==========================================================
    // HERO SECTION
    // ==========================================================

    private HeroSectionDto buildHero(Long userId) {

        User user = userRepo.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Resume latestResume =
                resumeRepository
                        .findFirstByUserIdOrderByCreatedAtDesc(userId);

        Double readiness =
                interviewSessionRepository
                        .findAverageScore(userId);

        int solvedQuestions =
                (int) progressRepository.countByUserId(userId);

        int completedInterviews =
                (int) interviewSessionRepository
                        .countByUserIdAndStatus(
                                userId,
                                InterviewStatus.COMPLETED
                        );

        int streak =
                calculateCurrentStreak(userId);

        int xp =
                calculateXP(
                        solvedQuestions,
                        completedInterviews,
                        readiness
                );

        return HeroSectionDto.builder()
                .fullName(user.getName())
                .targetCompany(
                        latestResume != null
                                ? latestResume.getCompany()
                                : "-"
                )
                .targetRole(
                        latestResume != null
                                ? latestResume.getJobRole()
                                : "-"
                )
                .dailyRecommendation(
                        buildRecommendation(
                                readiness,
                                solvedQuestions
                        )
                )
                .interviewReadiness(readiness)
                .currentStreak(streak)
                .xp(xp)
                .build();
    }

    // ==========================================================
    // KPI
    // ==========================================================

    private DashboardMetricsDto buildMetrics(Long userId) {

        Resume latestResume = resumeRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId);

        double atsScore = 0.0;
        double resumeQuality = 0.0;

        if (latestResume != null) {

            if (latestResume.getAtsScore() != null) {
                atsScore = latestResume
                        .getAtsScore()
                        .doubleValue();
            }

            resumeQuality =
                    extractResumeQuality(latestResume);
        }

        Double interviewReadiness =
                interviewSessionRepository
                        .findAverageScore(userId);

        int solvedQuestions =
                (int) progressRepository.countByUserId(userId);

        long totalQuestions =
                codingQuestionRepository.count();

        int completedInterviews =
                (int) interviewSessionRepository
                        .countByUserIdAndStatus(
                                userId,
                                InterviewStatus.COMPLETED
                        );

        int streak =
                calculateCurrentStreak(userId);

        int xp =
                calculateXP(
                        solvedQuestions,
                        completedInterviews,
                        interviewReadiness
                );

        double learningHours =
                calculateLearningHours(
                        solvedQuestions,
                        completedInterviews
                );

        double weeklyProgress =
                calculateWeeklyProgress(
                        solvedQuestions,
                        totalQuestions
                );

        double skillCoverage =
                calculateSkillCoverage(userId);

        return DashboardMetricsDto.builder()
                .atsScore(atsScore)
                .resumeQuality(resumeQuality)
                .interviewReadiness(interviewReadiness)
                .solvedQuestions(solvedQuestions)
                .learningHours(learningHours)
                .weeklyProgress(weeklyProgress)
                .skillCoverage(skillCoverage)
                .currentStreak(streak)
                .xp(xp)
                .build();
    }

    private double calculateLearningHours(
            int solved,
            int interviews) {

        return (solved * 0.25)
                + (interviews * 0.50);
    }

    private double calculateWeeklyProgress(
            int solved,
            long totalQuestions) {

        if (totalQuestions == 0) {
            return 0;
        }

        return (solved * 100.0)
                / totalQuestions;
    }

    private double calculateSkillCoverage(
            Long userId) {

        int skills =
                skillRepository.findByUserId(userId)
                        .size();

        int expectedSkills = 8;

        double percentage =
                (skills * 100.0)
                        / expectedSkills;

        return Math.min(
                percentage,
                100.0
        );
    }

    // ==========================================================
    // INTERVIEW TREND
    // ==========================================================

    private List<ReadinessTrendDto> buildInterviewTrend(Long userId) {

        List<InterviewSession> sessions =
                interviewSessionRepository
                        .findByUserIdAndStatusOrderByCreatedAtDesc(
                                userId,
                                InterviewStatus.COMPLETED
                        );

        if (sessions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<LocalDate, Double> trend =
                new TreeMap<>();

        for (InterviewSession session : sessions) {

            if (session.getOverallScore() == null) {
                continue;
            }

            LocalDate date =
                    session.getCreatedAt().toLocalDate();

            trend.merge(
                    date,
                    session.getOverallScore(),
                    Double::max
            );
        }

        return trend.entrySet()
                .stream()
                .map(entry ->
                        ReadinessTrendDto.builder()
                                .date(entry.getKey().toString())
                                .readinessScore(entry.getValue())
                                .targetScore(90.0)
                                .build()
                )
                .toList();
    }

    // ==========================================================
    // WEEKLY ACTIVITY
    // ==========================================================

    private List<WeeklyActivityDto> buildWeeklyActivity(Long userId) {

        LocalDate today = LocalDate.now();

        Map<DayOfWeek, WeeklyActivityDto> weeklyMap =
                new LinkedHashMap<>();

        for (DayOfWeek day : DayOfWeek.values()) {

            weeklyMap.put(day,
                    WeeklyActivityDto.builder()
                            .day(day.name().substring(0,3))
                            .solvedProblems(0)
                            .mockInterviews(0)
                            .hoursStudied(0.0)
                            .build());
        }

        // -----------------------------
        // Coding Activity
        // -----------------------------

        List<UserQuestionProgress> solved =
                progressRepository
                        .findByUserIdOrderBySolvedAtDesc(userId);

        for (UserQuestionProgress progress : solved) {

            LocalDate solvedDate =
                    progress.getSolvedAt().toLocalDate();

            if (solvedDate.isBefore(today.minusDays(6))) {
                continue;
            }

            DayOfWeek day =
                    solvedDate.getDayOfWeek();

            WeeklyActivityDto dto =
                    weeklyMap.get(day);

            dto.setSolvedProblems(
                    dto.getSolvedProblems() + 1);

            dto.setHoursStudied(
                    dto.getHoursStudied() + 0.25);
        }

        // -----------------------------
        // Interview Activity
        // -----------------------------

        List<InterviewSession> interviews =
                interviewSessionRepository
                        .findByUserIdAndStatusOrderByCreatedAtDesc(
                                userId,
                                InterviewStatus.COMPLETED
                        );

        for (InterviewSession session : interviews) {

            LocalDate interviewDate =
                    session.getCreatedAt().toLocalDate();

            if (interviewDate.isBefore(today.minusDays(6))) {
                continue;
            }

            DayOfWeek day =
                    interviewDate.getDayOfWeek();

            WeeklyActivityDto dto =
                    weeklyMap.get(day);

            dto.setMockInterviews(
                    dto.getMockInterviews() + 1);

            dto.setHoursStudied(
                    dto.getHoursStudied() + 0.50);
        }

        return new ArrayList<>(weeklyMap.values());
    }

    // ==========================================================
    // TOPIC DISTRIBUTION
    // ==========================================================

    private List<TopicDistributionDto> buildTopicDistribution(
            Long userId) {

        List<CodingTopic> rootTopics =
                codingTopicRepository
                        .findByParentIsNullOrderByDisplayOrderAsc();

        Map<Long, Long> solvedMap = new HashMap<>();

        List<Object[]> solved =
                progressRepository
                        .countSolvedByRootTopic(userId);

        for (Object[] row : solved) {

            Long topicId = (Long) row[0];

            Long count = (Long) row[1];

            solvedMap.put(topicId, count);
        }

        List<TopicDistributionDto> result =
                new ArrayList<>();

        for (CodingTopic topic : rootTopics) {

            long value =
                    solvedMap.getOrDefault(
                            topic.getId(),
                            0L
                    );

            result.add(
                    TopicDistributionDto
                            .builder()
                            .topic(topic.getName())
                            .value((int) value)
                            .build()
            );
        }

        return result;
    }

    // ==========================================================
    // SKILL RADAR
    // ==========================================================

    private List<SkillRadarDto> buildSkillRadar(Long userId) {

        List<SkillRadarDto> radar = new ArrayList<>();

        List<Skill> skills =
                skillRepository.findByUserId(userId);

        Set<String> userSkills =
                skills.stream()
                        .map(s -> s.getSkillName().toLowerCase())
                        .collect(Collectors.toSet());

        double interviewScore =
                Optional.ofNullable(
                        interviewSessionRepository
                                .findAverageScore(userId)
                ).orElse(0.0);

        long solved =
                progressRepository.countByUserId(userId);

        long totalQuestions =
                codingQuestionRepository.count();

        double dsaScore =
                totalQuestions == 0
                        ? 0
                        : Math.min(
                        100,
                        solved * 100.0 / totalQuestions
                );

        radar.add(buildSkill("Java", userSkills, interviewScore));
        radar.add(buildSkill("Spring Boot", userSkills, interviewScore));
        radar.add(buildSkill("React", userSkills, interviewScore));
        radar.add(buildSkill("SQL", userSkills, interviewScore));

        radar.add(
                SkillRadarDto.builder()
                        .skill("DSA")
                        .score(dsaScore)
                        .build()
        );

        radar.add(
                SkillRadarDto.builder()
                        .skill("System Design")
                        .score(interviewScore * 0.80)
                        .build()
        );

        radar.add(
                SkillRadarDto.builder()
                        .skill("Behavioral")
                        .score(interviewScore)
                        .build()
        );

        radar.add(
                SkillRadarDto.builder()
                        .skill("Communication")
                        .score(interviewScore * 0.90)
                        .build()
        );

        return radar;
    }

    private SkillRadarDto buildSkill(
            String name,
            Set<String> userSkills,
            double interviewScore) {

        double score;

        if (userSkills.contains(name.toLowerCase())) {

            score = Math.max(
                    70,
                    interviewScore
            );

        } else {

            score = interviewScore * 0.60;
        }

        return SkillRadarDto.builder()
                .skill(name)
                .score(Math.min(score, 100))
                .build();
    }


    // ==========================================================
    // ACTIVITIES
    // ==========================================================

    private List<ActivityDto> buildActivities(Long userId) {

        List<ActivityDto> activities = new ArrayList<>();

        // ------------------------------------
        // Resume Activity
        // ------------------------------------

        Resume latestResume =
                resumeRepository
                        .findFirstByUserIdOrderByCreatedAtDesc(userId);

        if (latestResume != null) {

            activities.add(
                    ActivityDto.builder()
                            .type("Resume")
                            .title("Resume Analyzed")
                            .description(
                                    latestResume.getCompany()
                                            + " - "
                                            + latestResume.getJobRole()
                            )
                            .createdAt(
                                    latestResume.getCreatedAt()
                            )
                            .build()
            );
        }

        // ------------------------------------
        // Interview Activity
        // ------------------------------------

        List<InterviewSession> interviews =
                interviewSessionRepository
                        .findByUserIdAndStatusOrderByCreatedAtDesc(
                                userId,
                                InterviewStatus.COMPLETED
                        );

        interviews.stream()
                .limit(5)
                .forEach(session ->

                        activities.add(
                                ActivityDto.builder()
                                        .type("Interview")
                                        .title("Interview Completed")
                                        .description(
                                                session.getCompanyName()
                                                        + " - "
                                                        + session.getJobRole()
                                        )
                                        .createdAt(
                                                session.getCreatedAt()
                                        )
                                        .build()
                        )

                );

        // ------------------------------------
        // Coding Activity
        // ------------------------------------

        List<UserQuestionProgress> solved =
                progressRepository
                        .findByUserIdOrderBySolvedAtDesc(userId);

        solved.stream()
                .limit(5)
                .forEach(progress ->

                        activities.add(
                                ActivityDto.builder()
                                        .type("Coding")
                                        .title("Solved Problem")
                                        .description(
                                                progress.getQuestion()
                                                        .getTitle()
                                        )
                                        .createdAt(
                                                progress.getSolvedAt()
                                        )
                                        .build()
                        )

                );

        activities.sort(
                Comparator.comparing(
                        ActivityDto::getCreatedAt
                ).reversed()
        );

        return activities.stream()
                .limit(10)
                .toList();
    }

    // ==========================================================
    // UPCOMING TASKS
    // ==========================================================

    private List<UpcomingTaskDto> buildUpcomingTasks(Long userId) {

        List<UpcomingTaskDto> tasks = new ArrayList<>();

        Resume latestResume =
                resumeRepository
                        .findFirstByUserIdOrderByCreatedAtDesc(userId);

        Double interviewScore =
                interviewSessionRepository
                        .findAverageScore(userId);

        long solved =
                progressRepository.countByUserId(userId);

        long totalQuestions =
                codingQuestionRepository.count();

        int skills =
                skillRepository.findByUserId(userId).size();

        // -----------------------------------------
        // Resume Improvement
        // -----------------------------------------

        if (latestResume == null) {

            tasks.add(
                    UpcomingTaskDto.builder()
                            .task("Upload your resume")
                            .priority("HIGH")
                            .progress(0)
                            .build()
            );

        } else if (latestResume.getAtsScore() != null
                && latestResume.getAtsScore().doubleValue() < 80) {

            tasks.add(
                    UpcomingTaskDto.builder()
                            .task("Improve ATS score above 80")
                            .priority("HIGH")
                            .progress(
                                    latestResume
                                            .getAtsScore()
                                            .intValue()
                            )
                            .build()
            );
        }

        // -----------------------------------------
        // Interview
        // -----------------------------------------

        if (interviewScore == null || interviewScore < 75) {

            tasks.add(
                    UpcomingTaskDto.builder()
                            .task("Complete a mock interview")
                            .priority("HIGH")
                            .progress(
                                    interviewScore == null
                                            ? 0
                                            : interviewScore.intValue()
                            )
                            .build()
            );
        }

        // -----------------------------------------
        // Coding
        // -----------------------------------------

        if (totalQuestions > 0) {

            int codingProgress =
                    (int) ((solved * 100) / totalQuestions);

            if (codingProgress < 60) {

                tasks.add(
                        UpcomingTaskDto.builder()
                                .task("Solve more coding problems")
                                .priority("MEDIUM")
                                .progress(codingProgress)
                                .build()
                );
            }
        }

        // -----------------------------------------
        // Skills
        // -----------------------------------------

        if (skills < 8) {

            int progress =
                    (skills * 100) / 8;

            tasks.add(
                    UpcomingTaskDto.builder()
                            .task("Add more profile skills")
                            .priority("LOW")
                            .progress(progress)
                            .build()
            );
        }

        return tasks;
    }

    private double extractResumeQuality(
            Resume resume) {

        if (resume.getAnalysisJson() == null) {
            return 0.0;
        }

        if (resume.getAnalysisJson()
                .has("resumeQuality")) {

            return resume
                    .getAnalysisJson()
                    .get("resumeQuality")
                    .asDouble();
        }

        return 0.0;
    }

    // ==========================================================
    // HELPERS
    // ==========================================================

    private int calculateXP(
            int solved,
            int interviews,
            Double readiness) {

        int xp = 0;

        xp += solved * 10;

        xp += interviews * 50;

        if (readiness != null) {
            xp += readiness.intValue();
        }

        return xp;
    }

    private int calculateCurrentStreak(Long userId) {

        List<UserQuestionProgress> solved =
                progressRepository
                        .findByUserIdOrderBySolvedAtDesc(userId);

        if (solved.isEmpty()) {
            return 0;
        }

        Set<LocalDate> solvedDays = solved.stream()
                .map(p -> p.getSolvedAt().toLocalDate())
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();

        int streak = 0;

        while (solvedDays.contains(today)) {
            streak++;
            today = today.minusDays(1);
        }

        return streak;
    }
    private String buildRecommendation(
            Double readiness,
            int solved) {

        if (readiness == null) {
            return "Complete your first interview.";
        }

        if (readiness < 60) {
            return "Focus on interview preparation.";
        }

        if (solved < 100) {
            return "Practice more coding problems.";
        }

        if (readiness < 80) {
            return "Improve system design and behavioural skills.";
        }

        return "You're interview ready. Keep practicing consistently.";
    }

}
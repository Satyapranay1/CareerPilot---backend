package com.example.careerpilot.controller;

import com.example.careerpilot.dto.*;
import com.example.careerpilot.model.Education;
import com.example.careerpilot.model.Experience;
import com.example.careerpilot.model.Skill;
import com.example.careerpilot.model.User;
import com.example.careerpilot.repo.EducationRepo;
import com.example.careerpilot.repo.ExperienceRepo;
import com.example.careerpilot.repo.SkillRepo;
import com.example.careerpilot.repo.UserRepo;
import com.example.careerpilot.security.JwtUtils;
import com.example.careerpilot.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    private final EducationRepo educationRepo;
    private final ExperienceRepo experienceRepo;
    private final SkillRepo skillRepo;


    // ========================= REGISTER =========================

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequestDTO request) {

        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Email already registered")
            );
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        User savedUser = userRepo.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "message", "User registered successfully",
                        "id", savedUser.getId(),
                        "name", savedUser.getName(),
                        "email", savedUser.getEmail()
                )
        );
    }


    // ========================= LOGIN =========================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestDTO request) {

        User user = userRepo.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null ||
                !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash())) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Invalid email or password"
                    ));
        }

        if (!user.getEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message", "Account is disabled"
                    ));
        }

        String token = jwtUtils.generateToken((user.getId()));

        return ResponseEntity.ok(
                Map.of(
                        "message", "Login successful",
                        "token", token,
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "role", user.getRole().name()
                )
        );
    }


    // ========================= ME =========================

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(
                Map.of(
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail()
                )
        );
    }


    // ========================= PROFILE =========================

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        ProfileResponseDTO response = new ProfileResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getShortBio(),
                user.getRole().name(),
                user.getProfilePicture(),
                educationRepo.findByUserId(user.getId()),
                experienceRepo.findByUserId(user.getId()),
                skillRepo.findByUserId(user.getId())
        );
        return ResponseEntity.ok(response);
    }


    // ========================= BIO =========================

    @PutMapping("/bio")
    public ResponseEntity<?> updateBio(
            @Valid @RequestBody BioRequestDTO requestDTO,
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        user.setShortBio(requestDTO.getShortBio());

        userRepo.save(user);

        return ResponseEntity.ok(
                Map.of(
                        "message", "Bio updated successfully",
                        "shortBio", user.getShortBio()
                )
        );
    }


    // ========================= EDUCATION =========================

    @GetMapping("/education")
    public ResponseEntity<?> getEducation(
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(
                educationRepo.findByUserId(user.getId())
        );
    }


    @PostMapping("/education")
    public ResponseEntity<?> addEducation(
            @Valid @RequestBody EducationRequestDTO requestDTO,
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        Education education = new Education();

        education.setUserId(user.getId());
        education.setInstitution(requestDTO.getInstitution());
        education.setDegree(requestDTO.getDegree());
        education.setFieldOfStudy(requestDTO.getFieldOfStudy());
        education.setStartYear(requestDTO.getStartYear());
        education.setEndYear(requestDTO.getEndYear());
        education.setGrade(requestDTO.getGrade());

        Education savedEducation =
                educationRepo.save(education);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedEducation);
    }


    @PutMapping("/education/{id}")
    public ResponseEntity<?> updateEducation(
            @PathVariable Long id,
            @Valid @RequestBody EducationRequestDTO requestDTO,
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        Education education = educationRepo.findById(id)
                .orElse(null);

        if (education == null ||
                !education.getUserId().equals(user.getId())) {

            return ResponseEntity.notFound().build();
        }

        education.setInstitution(requestDTO.getInstitution());
        education.setDegree(requestDTO.getDegree());
        education.setFieldOfStudy(requestDTO.getFieldOfStudy());
        education.setStartYear(requestDTO.getStartYear());
        education.setEndYear(requestDTO.getEndYear());
        education.setGrade(requestDTO.getGrade());

        return ResponseEntity.ok(
                educationRepo.save(education)
        );
    }


    @DeleteMapping("/education/{id}")
    public ResponseEntity<?> deleteEducation(
            @PathVariable Long id,
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        Education education = educationRepo.findById(id)
                .orElse(null);

        if (education == null ||
                !education.getUserId().equals(user.getId())) {

            return ResponseEntity.notFound().build();
        }

        educationRepo.delete(education);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Education deleted successfully"
                )
        );
    }


    // ========================= EXPERIENCE =========================

    @GetMapping("/experience")
    public ResponseEntity<?> getExperience(
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(
                experienceRepo.findByUserId(user.getId())
        );
    }


    @PostMapping("/experience")
    public ResponseEntity<?> addExperience(
            @Valid @RequestBody ExperienceRequestDTO requestDTO,
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        if (requestDTO.getEndDate() != null &&
                requestDTO.getEndDate()
                        .isBefore(requestDTO.getStartDate())) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "End date cannot be before start date"
                    )
            );
        }

        Experience experience = new Experience();

        experience.setUserId(user.getId());
        experience.setCompany(requestDTO.getCompany());
        experience.setJobTitle(requestDTO.getJobTitle());
        experience.setStartDate(requestDTO.getStartDate());
        experience.setEndDate(requestDTO.getEndDate());
        experience.setDescription(requestDTO.getDescription());

        Experience savedExperience =
                experienceRepo.save(experience);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedExperience);
    }


    @PutMapping("/experience/{id}")
    public ResponseEntity<?> updateExperience(
            @PathVariable Long id,
            @Valid @RequestBody ExperienceRequestDTO requestDTO,
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        Experience experience = experienceRepo.findById(id)
                .orElse(null);

        if (experience == null ||
                !experience.getUserId().equals(user.getId())) {

            return ResponseEntity.notFound().build();
        }

        if (requestDTO.getEndDate() != null &&
                requestDTO.getEndDate()
                        .isBefore(requestDTO.getStartDate())) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "End date cannot be before start date"
                    )
            );
        }

        experience.setCompany(requestDTO.getCompany());
        experience.setJobTitle(requestDTO.getJobTitle());
        experience.setStartDate(requestDTO.getStartDate());
        experience.setEndDate(requestDTO.getEndDate());
        experience.setDescription(requestDTO.getDescription());

        return ResponseEntity.ok(
                experienceRepo.save(experience)
        );
    }


    @DeleteMapping("/experience/{id}")
    public ResponseEntity<?> deleteExperience(
            @PathVariable Long id,
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        Experience experience = experienceRepo.findById(id)
                .orElse(null);

        if (experience == null ||
                !experience.getUserId().equals(user.getId())) {

            return ResponseEntity.notFound().build();
        }

        experienceRepo.delete(experience);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Experience deleted successfully"
                )
        );
    }


    // ========================= SKILLS =========================

    @GetMapping("/skills")
    public ResponseEntity<?> getSkills(
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(
                skillRepo.findByUserId(user.getId())
        );
    }


    @PostMapping("/skills")
    public ResponseEntity<?> addSkill(
            @Valid @RequestBody SkillRequestDTO requestDTO,
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        if (skillRepo.existsByUserIdAndSkillNameIgnoreCase(
                user.getId(),
                requestDTO.getSkillName())) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Skill already exists"
                    )
            );
        }

        Skill skill = new Skill();

        skill.setUserId(user.getId());
        skill.setSkillName(requestDTO.getSkillName());

        Skill savedSkill = skillRepo.save(skill);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedSkill);
    }


    @PutMapping("/skills/{id}")
    public ResponseEntity<?> updateSkill(
            @PathVariable Long id,
            @Valid @RequestBody SkillRequestDTO requestDTO,
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        Skill skill = skillRepo.findById(id)
                .orElse(null);

        if (skill == null ||
                !skill.getUserId().equals(user.getId())) {

            return ResponseEntity.notFound().build();
        }

        if (!skill.getSkillName()
                .equalsIgnoreCase(requestDTO.getSkillName())
                &&
                skillRepo.existsByUserIdAndSkillNameIgnoreCase(
                        user.getId(),
                        requestDTO.getSkillName())) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Skill already exists"
                    )
            );
        }

        skill.setSkillName(requestDTO.getSkillName());

        return ResponseEntity.ok(
                skillRepo.save(skill)
        );
    }


    @DeleteMapping("/skills/{id}")
    public ResponseEntity<?> deleteSkill(
            @PathVariable Long id,
            HttpServletRequest request) {

        User user = getUser(request);

        if (user == null) {
            return unauthorized();
        }

        Skill skill = skillRepo.findById(id)
                .orElse(null);

        if (skill == null ||
                !skill.getUserId().equals(user.getId())) {

            return ResponseEntity.notFound().build();
        }

        skillRepo.delete(skill);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Skill deleted successfully"
                )
        );
    }


    // ========================= COMMON =========================

    private User getUser(HttpServletRequest request) {

        return userService
                .getUserFromRequest(request)
                .orElse(null);
    }


    private ResponseEntity<?> unauthorized() {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "message",
                        "Unauthorized"
                ));
    }
}
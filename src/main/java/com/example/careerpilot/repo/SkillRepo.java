package com.example.careerpilot.repo;

import com.example.careerpilot.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillRepo extends JpaRepository<Skill, Long> {

    List<Skill> findByUserId(Long userId);

    boolean existsByUserIdAndSkillNameIgnoreCase(
            Long userId,
            String skillName
    );
}
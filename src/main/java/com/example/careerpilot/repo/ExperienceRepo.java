package com.example.careerpilot.repo;

import com.example.careerpilot.model.Experience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperienceRepo extends JpaRepository<Experience, Long> {

    List<Experience> findByUserId(Long userId);
}
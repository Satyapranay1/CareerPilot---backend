package com.example.careerpilot.repo;

import com.example.careerpilot.model.Education;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EducationRepo extends JpaRepository<Education, Long> {

    List<Education> findByUserId(Long userId);
}
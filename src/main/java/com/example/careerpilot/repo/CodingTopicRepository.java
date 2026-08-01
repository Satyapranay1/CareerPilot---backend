package com.example.careerpilot.repo;

import com.example.careerpilot.model.CodingTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodingTopicRepository
        extends JpaRepository<CodingTopic, Long> {

    List<CodingTopic> findByParentIsNullOrderByDisplayOrderAsc();

    List<CodingTopic> findByParentIdOrderByDisplayOrderAsc(Long parentId);
}
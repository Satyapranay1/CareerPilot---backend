package com.example.careerpilot.dto;

import com.example.careerpilot.model.Education;
import com.example.careerpilot.model.Experience;
import com.example.careerpilot.model.Skill;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProfileResponseDTO {

    private Long id;

    private String name;

    private String email;

    private String shortBio;

    private String role;

    private String profilePicture;

    private List<Education> education;

    private List<Experience> experience;

    private List<Skill> skills;
}
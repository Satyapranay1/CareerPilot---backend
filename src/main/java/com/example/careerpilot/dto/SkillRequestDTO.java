package com.example.careerpilot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillRequestDTO {

    @NotBlank
    private String skillName;
}
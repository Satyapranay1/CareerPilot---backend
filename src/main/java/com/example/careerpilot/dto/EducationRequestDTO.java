package com.example.careerpilot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EducationRequestDTO {

    @NotBlank
    private String institution;

    private String degree;
    private String fieldOfStudy;
    private Integer startYear;
    private Integer endYear;
    private String grade;
}
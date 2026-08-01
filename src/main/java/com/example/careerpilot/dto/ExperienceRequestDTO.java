package com.example.careerpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ExperienceRequestDTO {

    @NotBlank
    private String company;

    @NotBlank
    private String jobTitle;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    private String description;
}
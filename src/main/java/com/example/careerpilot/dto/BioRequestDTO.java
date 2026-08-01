package com.example.careerpilot.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BioRequestDTO {

    @Size(max = 500)
    private String shortBio;
}
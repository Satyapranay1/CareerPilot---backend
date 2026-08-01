package com.example.careerpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleExpectation {

    private String keyword;

    private String category;

    private int importance;

    private String description;
}
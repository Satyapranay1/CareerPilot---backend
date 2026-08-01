package com.example.careerpilot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class RegisterRequestDTO {
        @NotBlank
        private String name;


        @Email
        private String email;


        @NotBlank
        private String password;
}
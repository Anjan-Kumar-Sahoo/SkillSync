package com.skillsync.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InitiateRegistrationRequest(
        @NotBlank @Email String email
) {}

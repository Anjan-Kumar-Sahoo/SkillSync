package com.skillsync.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    String email
) {}

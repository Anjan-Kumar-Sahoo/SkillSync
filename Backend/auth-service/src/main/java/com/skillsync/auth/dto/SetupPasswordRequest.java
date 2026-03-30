package com.skillsync.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for setting up a permanent password after OAuth registration.
 * This is a mandatory step for users who registered via OAuth.
 */
public record SetupPasswordRequest(
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password
) {}

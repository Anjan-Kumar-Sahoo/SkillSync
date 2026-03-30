package com.skillsync.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OAuthRequest(
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "First name is mandatory")
    String firstName,

    @NotBlank(message = "Last name is mandatory")
    String lastName,

    @NotBlank(message = "Provider is mandatory")
    String provider,

    @NotBlank(message = "ProviderId is mandatory")
    String providerId
) {}

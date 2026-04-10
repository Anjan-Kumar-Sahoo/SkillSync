package com.skillsync.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "OTP is mandatory")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    String otp,

    @NotBlank(message = "New password is mandatory")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String newPassword
) {}

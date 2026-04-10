package com.skillsync.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @Email(message = "Invalid email format")
    String email,

    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    String otp,

    @NotBlank(message = "New password is mandatory")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String newPassword,

    @Size(min = 8, max = 100, message = "Confirm password must be between 8 and 100 characters")
    String confirmPassword,

    @Size(min = 8, max = 100, message = "Current password must be between 8 and 100 characters")
    String currentPassword
) {}

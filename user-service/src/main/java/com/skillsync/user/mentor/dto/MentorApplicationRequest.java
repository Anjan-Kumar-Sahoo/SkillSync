package com.skillsync.user.mentor.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record MentorApplicationRequest(
        @NotBlank @Size(min = 50, max = 2000) String bio,
        @Min(0) @Max(50) int experienceYears,
        @DecimalMin("5.00") @DecimalMax("500.00") BigDecimal hourlyRate,
        @NotEmpty @Size(max = 10) List<Long> skillIds
) {}

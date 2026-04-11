package com.skillsync.session.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record CreateSessionRequest(
        @NotNull Long mentorId,
        @NotBlank String topic,
        String description,
        @Future LocalDateTime sessionDate,
        @Min(30) @Max(120) int durationMinutes
) {}

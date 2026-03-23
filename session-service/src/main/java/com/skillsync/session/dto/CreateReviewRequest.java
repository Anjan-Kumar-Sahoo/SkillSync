package com.skillsync.session.dto;

import jakarta.validation.constraints.*;

public record CreateReviewRequest(
    @NotNull Long sessionId,
    @Min(1) @Max(5) int rating,
    @Size(min = 10, max = 2000) String comment
) {}

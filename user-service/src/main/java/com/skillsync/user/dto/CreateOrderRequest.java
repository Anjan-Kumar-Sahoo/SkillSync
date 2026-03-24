package com.skillsync.user.dto;

import com.skillsync.user.enums.PaymentType;
import com.skillsync.user.enums.ReferenceType;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull(message = "Payment type is required")
        PaymentType type,

        @NotNull(message = "Reference ID is required — identifies the business entity this payment is for")
        Long referenceId,

        @NotNull(message = "Reference type is required — MENTOR_ONBOARDING or SESSION_BOOKING")
        ReferenceType referenceType
) {}

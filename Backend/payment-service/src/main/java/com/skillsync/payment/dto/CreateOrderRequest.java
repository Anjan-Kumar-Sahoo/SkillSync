package com.skillsync.payment.dto;

import com.skillsync.payment.enums.PaymentType;
import com.skillsync.payment.enums.ReferenceType;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull(message = "Payment type is required")
        PaymentType type,

        @NotNull(message = "Reference ID is required — identifies the business entity this payment is for")
        Long referenceId,

        @NotNull(message = "Reference type is required — MENTOR_ONBOARDING or SESSION_BOOKING")
        ReferenceType referenceType
) {}

package com.skillsync.user.dto;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long userId,
        String type,
        int amount,
        String razorpayOrderId,
        String razorpayPaymentId,
        String status,
        Long referenceId,
        String referenceType,
        String compensationReason,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {}

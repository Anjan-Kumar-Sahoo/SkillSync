package com.skillsync.payment.mapper;

import com.skillsync.payment.dto.PaymentResponse;
import com.skillsync.payment.entity.Payment;

/**
 * Pure mapping functions for Payment entities.
 * Used by PaymentService for response conversion.
 * NO service calls, NO repository calls — ONLY transformation logic.
 */
public final class PaymentMapper {

    private PaymentMapper() {}

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getType().name(),
                payment.getAmount(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getStatus().name(),
                payment.getReferenceId(),
                payment.getReferenceType() != null ? payment.getReferenceType().name() : null,
                payment.getCompensationReason(),
                payment.getCreatedAt(),
                payment.getCompletedAt()
        );
    }
}

package com.skillsync.session.event;

public record PaymentCompletedEvent(
        String eventId,
        String version,
        String timestamp,
        Long userId,
        String orderId,
        String type,
        String status,
        int amount,
        Long referenceId,
        String referenceType,
        String compensationReason
) {}

package com.skillsync.user.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a payment reaches a terminal state (SUCCESS, FAILED, COMPENSATED).
 * Consumed by Notification Service to push payment status notifications to the user.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCompletedEvent {

    private Long userId;
    private String orderId;
    private String paymentType;       // MENTOR_FEE, SESSION_BOOKING
    private String status;            // SUCCESS, FAILED, COMPENSATED
    private Integer amount;           // in paise
    private Long referenceId;
    private String referenceType;     // MENTOR_ONBOARDING, SESSION_BOOKING
    private String compensationReason; // only set if COMPENSATED
}

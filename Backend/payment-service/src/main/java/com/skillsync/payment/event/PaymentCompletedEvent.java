package com.skillsync.payment.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a payment reaches a terminal state (SUCCESS, FAILED, COMPENSATED).
 *
 * <h3>Event Versioning & Schema Evolution:</h3>
 * <ul>
 *   <li>{@code eventId} — globally unique UUID for idempotency/deduplication</li>
 *   <li>{@code version} — schema version for backward compatibility (currently "1")</li>
 *   <li>{@code timestamp} — ISO-8601 timestamp of event creation</li>
 * </ul>
 *
 * <h3>Schema Evolution Rules (Additive Only):</h3>
 * <ul>
 *   <li>✅ New fields may be added, but MUST be optional (nullable)</li>
 *   <li>❌ Existing fields MUST NOT be removed or renamed</li>
 *   <li>❌ Field types MUST NOT be changed</li>
 *   <li>When adding fields, increment the version (v1 → v2) and publish on a new routing key</li>
 *   <li>Consumers MUST ignore unknown fields ({@code @JsonIgnoreProperties(ignoreUnknown = true)})</li>
 * </ul>
 *
 * <h3>Consumed by:</h3>
 * <ul>
 *   <li>Notification Service: push payment status notifications to the user</li>
 *   <li>User Service: trigger business actions (mentor approval, session booking)</li>
 * </ul>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentCompletedEvent {

    // ─── Event Envelope (versioning + deduplication) ───
    private String eventId;           // UUID for idempotency
    private String version;           // Schema version (e.g., "1")
    private String timestamp;         // ISO-8601 event creation time

    // ─── Business Payload ───
    private Long userId;
    private String orderId;
    private String paymentType;       // MENTOR_FEE, SESSION_BOOKING
    private String status;            // SUCCESS, FAILED, COMPENSATED
    private Integer amount;           // in paise
    private Long referenceId;
    private String referenceType;     // MENTOR_ONBOARDING, SESSION_BOOKING
    private String compensationReason; // only set if COMPENSATED
}

package com.skillsync.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks processed event IDs for consumer-level idempotency.
 * Before processing any incoming event, the consumer checks
 * if the eventId already exists in this table. If it does,
 * the event is skipped (duplicate).
 */
@Entity
@Table(name = "processed_events", schema = "users", indexes = {
        @Index(name = "idx_processed_event_id", columnList = "eventId", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Globally unique event ID (UUID from the producer) */
    @Column(nullable = false, unique = true, length = 64)
    private String eventId;

    /** Event type for debugging (e.g., payment.business.action) */
    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false)
    private LocalDateTime processedAt;
}

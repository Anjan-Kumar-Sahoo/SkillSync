package com.skillsync.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Persists failed DLQ events for admin review and safe replay.
 * Each failed consumer processing attempt stores the full event payload,
 * error details, and replay status.
 */
@Entity
@Table(name = "failed_events", schema = "payments", indexes = {
        @Index(name = "idx_failed_event_id", columnList = "eventId", unique = true),
        @Index(name = "idx_failed_status", columnList = "replayStatus"),
        @Index(name = "idx_failed_queue", columnList = "sourceQueue")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Original event ID (for deduplication + replay) */
    @Column(nullable = false, unique = true, length = 64)
    private String eventId;

    /** Source DLQ queue name */
    @Column(nullable = false, length = 100)
    private String sourceQueue;

    /** Original routing key */
    @Column(length = 100)
    private String routingKey;

    /** Full serialized event payload */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** Error reason that caused DLQ routing */
    @Column(length = 1000)
    private String errorReason;

    /** PENDING_REVIEW → REPLAYED / SKIPPED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReplayStatus replayStatus = ReplayStatus.PENDING_REVIEW;

    @Column(nullable = false)
    private LocalDateTime failedAt;

    private LocalDateTime replayedAt;

    public enum ReplayStatus {
        PENDING_REVIEW,
        REPLAYED,
        SKIPPED
    }
}

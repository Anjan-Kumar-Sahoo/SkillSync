package com.skillsync.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Transactional Outbox table for reliable event publishing.
 * Events are written to this table in the SAME transaction as the business DB write,
 * and a background publisher polls and sends them to RabbitMQ.
 *
 * <h3>Life Cycle (with Publisher Confirms):</h3>
 * <ol>
 *   <li>Business operation → save entity + insert OutboxEvent as PENDING (single TX)</li>
 *   <li>OutboxPublisher claims rows: PENDING → PROCESSING (with {@code FOR UPDATE SKIP LOCKED})</li>
 *   <li>Publishes to RabbitMQ and waits for broker ACK</li>
 *   <li>On ACK → PROCESSING → SENT</li>
 *   <li>On NACK / timeout → PROCESSING → FAILED (retried on next poll, up to 5 retries)</li>
 * </ol>
 */
@Entity
@Table(name = "outbox_events", schema = "payments", indexes = {
        @Index(name = "idx_outbox_status", columnList = "status"),
        @Index(name = "idx_outbox_created", columnList = "createdAt"),
        @Index(name = "idx_outbox_status_retry", columnList = "status, retryCount")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Globally unique event identifier for deduplication */
    @Column(nullable = false, unique = true, length = 64)
    private String eventId;

    /** Event type (e.g., payment.business.action, payment.success, payment.failed) */
    @Column(nullable = false, length = 100)
    private String eventType;

    /** Routing key for RabbitMQ */
    @Column(nullable = false, length = 100)
    private String routingKey;

    /** Exchange name for RabbitMQ */
    @Column(nullable = false, length = 100)
    private String exchange;

    /** Serialized JSON payload */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** PENDING → PROCESSING → SENT  or  PENDING → PROCESSING → FAILED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    /** Number of publish attempts */
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /** Last error message (for debugging failed publishes) */
    @Column(length = 500)
    private String lastError;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    /** Timestamp of the last publish attempt (for stale PROCESSING detection) */
    private LocalDateTime lastAttemptAt;

    public enum OutboxStatus {
        /** Written by business TX, waiting for publisher to claim */
        PENDING,
        /** Claimed by a publisher instance, broker ACK awaited */
        PROCESSING,
        /** Broker ACK received — delivery confirmed */
        SENT,
        /** Publish failed (NACK, timeout, or exception) — will be retried */
        FAILED
    }
}

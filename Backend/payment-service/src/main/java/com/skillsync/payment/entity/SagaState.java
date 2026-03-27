package com.skillsync.payment.entity;

import com.skillsync.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Persisted Saga state for payment orchestration.
 * Enables recovery after service restart — incomplete sagas
 * can be detected and resumed.
 *
 * <h3>State Tracking:</h3>
 * <ul>
 *   <li>Each saga step persists the current state</li>
 *   <li>Retry count is tracked for compensation decisions</li>
 *   <li>On restart, PENDING sagas are detected and retried</li>
 * </ul>
 */
@Entity
@Table(name = "saga_state", schema = "payments", indexes = {
        @Index(name = "idx_saga_payment_id", columnList = "paymentId", unique = true),
        @Index(name = "idx_saga_state", columnList = "state")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The payment this saga belongs to */
    @Column(nullable = false, unique = true)
    private Long paymentId;

    /** Razorpay order ID for logging/debugging */
    @Column(nullable = false, length = 64)
    private String orderId;

    /** Current saga state — mirrors PaymentStatus */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus state;

    /** Number of saga execution attempts */
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /** Last error message if saga failed */
    @Column(length = 1000)
    private String lastError;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.lastUpdated = now;
    }

    @PreUpdate
    void preUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}

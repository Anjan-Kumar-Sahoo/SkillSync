package com.skillsync.user.entity;

import com.skillsync.user.enums.PaymentStatus;
import com.skillsync.user.enums.PaymentType;
import com.skillsync.user.enums.ReferenceType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments", schema = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "razorpayOrderId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentType type;

    @Column(nullable = false)
    private Integer amount; // amount in paise (900 = ₹9)

    @Column(nullable = false, unique = true, length = 64)
    private String razorpayOrderId;

    @Column(length = 64)
    private String razorpayPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    // ─── Reference Mapping (CRITICAL for traceability) ───

    /** Business reference ID — e.g. mentorProfileId or sessionRequestId */
    @Column(nullable = false)
    private Long referenceId;

    /** Type of business operation this payment is linked to */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReferenceType referenceType;

    // ─── Compensation Tracking ───

    /** Reason for compensation if status = COMPENSATED */
    @Column(length = 500)
    private String compensationReason;

    // ─── Audit Fields ───

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}

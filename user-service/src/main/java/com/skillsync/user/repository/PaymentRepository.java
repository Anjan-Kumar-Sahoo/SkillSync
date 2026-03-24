package com.skillsync.user.repository;

import com.skillsync.user.entity.Payment;
import com.skillsync.user.enums.PaymentStatus;
import com.skillsync.user.enums.PaymentType;
import com.skillsync.user.enums.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    boolean existsByRazorpayOrderIdAndStatus(String razorpayOrderId, PaymentStatus status);

    List<Payment> findByUserIdAndTypeAndStatus(Long userId, PaymentType type, PaymentStatus status);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Check for duplicate payments on the same reference */
    List<Payment> findByReferenceIdAndReferenceTypeAndStatusIn(
            Long referenceId, ReferenceType referenceType, List<PaymentStatus> statuses);
}

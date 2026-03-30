package com.skillsync.payment.mapper;

import com.skillsync.payment.dto.PaymentResponse;
import com.skillsync.payment.entity.Payment;
import com.skillsync.payment.enums.PaymentStatus;
import com.skillsync.payment.enums.PaymentType;
import com.skillsync.payment.enums.ReferenceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PaymentMapper — pure function, no Spring context needed.
 */
class PaymentMapperTest {

    @Test
    @DisplayName("should map Payment entity to PaymentResponse with all fields")
    void mapPayment_full() {
        LocalDateTime now = LocalDateTime.now();
        Payment payment = Payment.builder()
                .id(1L).userId(100L)
                .type(PaymentType.SESSION_BOOKING)
                .amount(900)
                .razorpayOrderId("order_123")
                .razorpayPaymentId("pay_456")
                .status(PaymentStatus.SUCCESS)
                .referenceId(10L)
                .referenceType(ReferenceType.SESSION_BOOKING)
                .createdAt(now)
                .completedAt(now.plusMinutes(1))
                .build();

        PaymentResponse response = PaymentMapper.toResponse(payment);

        assertEquals(1L, response.id());
        assertEquals(100L, response.userId());
        assertEquals("SESSION_BOOKING", response.type());
        assertEquals(900, response.amount());
        assertEquals("order_123", response.razorpayOrderId());
        assertEquals("pay_456", response.razorpayPaymentId());
        assertEquals("SUCCESS", response.status());
        assertEquals(10L, response.referenceId());
        assertEquals("SESSION_BOOKING", response.referenceType());
        assertNull(response.compensationReason());
        assertEquals(now, response.createdAt());
        assertEquals(now.plusMinutes(1), response.completedAt());
    }

    @Test
    @DisplayName("should handle null referenceType gracefully")
    void mapPayment_nullReferenceType() {
        Payment payment = Payment.builder()
                .id(2L).userId(200L)
                .type(PaymentType.SESSION_BOOKING)
                .amount(900).status(PaymentStatus.CREATED)
                .build();

        PaymentResponse response = PaymentMapper.toResponse(payment);

        assertNull(response.referenceType());
        assertNull(response.compensationReason());
        assertNull(response.createdAt());
        assertNull(response.completedAt());
    }

    @Test
    @DisplayName("should map compensated payment with reason")
    void mapPayment_compensated() {
        Payment payment = Payment.builder()
                .id(3L).userId(300L)
                .type(PaymentType.SESSION_BOOKING)
                .amount(900)
                .razorpayOrderId("order_789")
                .status(PaymentStatus.COMPENSATED)
                .referenceId(20L)
                .referenceType(ReferenceType.SESSION_BOOKING)
                .compensationReason("Business action failed: Mentor not found")
                .completedAt(LocalDateTime.now())
                .build();

        PaymentResponse response = PaymentMapper.toResponse(payment);

        assertEquals("COMPENSATED", response.status());
        assertEquals("Business action failed: Mentor not found", response.compensationReason());
    }
}

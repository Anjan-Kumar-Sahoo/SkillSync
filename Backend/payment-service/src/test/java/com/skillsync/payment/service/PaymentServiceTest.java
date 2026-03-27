package com.skillsync.payment.service;

import com.skillsync.payment.dto.CreateOrderRequest;
import com.skillsync.payment.entity.Payment;
import com.skillsync.payment.enums.PaymentStatus;
import com.skillsync.payment.enums.PaymentType;
import com.skillsync.payment.enums.ReferenceType;
import com.skillsync.payment.exception.PaymentException;
import com.skillsync.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private com.razorpay.RazorpayClient razorpayClient;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentSagaOrchestrator sagaOrchestrator;
    @Mock private OutboxEventService outboxEventService;

    @InjectMocks private PaymentService paymentService;

    @Test
    @DisplayName("getUserPayments - should return all user payments")
    void getUserPayments_shouldReturnPayments() {
        Payment payment = Payment.builder()
                .id(1L).userId(100L)
                .type(PaymentType.MENTOR_FEE)
                .amount(900).status(PaymentStatus.SUCCESS)
                .razorpayOrderId("order_123")
                .referenceId(10L)
                .referenceType(ReferenceType.MENTOR_ONBOARDING)
                .build();

        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(payment));

        var result = paymentService.getUserPayments(100L);

        assertEquals(1, result.size());
        assertEquals("MENTOR_FEE", result.get(0).type());
    }

    @Test
    @DisplayName("getPaymentByOrderId - should return payment for valid owner")
    void getPaymentByOrderId_validOwner_shouldReturn() {
        Payment payment = Payment.builder()
                .id(1L).userId(100L)
                .type(PaymentType.MENTOR_FEE).amount(900)
                .razorpayOrderId("order_123")
                .status(PaymentStatus.SUCCESS)
                .referenceId(10L)
                .referenceType(ReferenceType.MENTOR_ONBOARDING)
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_123"))
                .thenReturn(Optional.of(payment));

        var result = paymentService.getPaymentByOrderId(100L, "order_123");

        assertEquals("order_123", result.razorpayOrderId());
    }

    @Test
    @DisplayName("getPaymentByOrderId - should reject unauthorized access")
    void getPaymentByOrderId_wrongUser_shouldThrow() {
        Payment payment = Payment.builder()
                .id(1L).userId(100L)
                .type(PaymentType.MENTOR_FEE).amount(900)
                .razorpayOrderId("order_123")
                .status(PaymentStatus.SUCCESS)
                .referenceId(10L).referenceType(ReferenceType.MENTOR_ONBOARDING)
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_123"))
                .thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class, () ->
                paymentService.getPaymentByOrderId(999L, "order_123"));
    }

    @Test
    @DisplayName("hasSuccessfulPayment - should return true when exists")
    void hasSuccessfulPayment_exists_shouldReturnTrue() {
        when(paymentRepository.findByUserIdAndTypeAndStatus(100L, PaymentType.MENTOR_FEE, PaymentStatus.SUCCESS))
                .thenReturn(List.of(Payment.builder().build()));

        assertTrue(paymentService.hasSuccessfulPayment(100L, PaymentType.MENTOR_FEE));
    }

    @Test
    @DisplayName("hasSuccessfulPayment - should return false when not exists")
    void hasSuccessfulPayment_notExists_shouldReturnFalse() {
        when(paymentRepository.findByUserIdAndTypeAndStatus(100L, PaymentType.MENTOR_FEE, PaymentStatus.SUCCESS))
                .thenReturn(List.of());

        assertFalse(paymentService.hasSuccessfulPayment(100L, PaymentType.MENTOR_FEE));
    }
}

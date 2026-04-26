package com.skillsync.payment.service;

import com.skillsync.payment.dto.CreateOrderRequest;
import com.skillsync.payment.dto.VerifyPaymentRequest;
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

import org.springframework.http.HttpStatus;

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

        private Payment basePayment(PaymentStatus status, Long userId) {
                return Payment.builder()
                                .id(1L)
                                .userId(userId)
                                .type(PaymentType.SESSION_BOOKING)
                                .amount(900)
                                .razorpayOrderId("order_123")
                                .status(status)
                                .referenceId(10L)
                                .referenceType(ReferenceType.SESSION_BOOKING)
                                .build();
        }

    @Test
    @DisplayName("getUserPayments - should return all user payments")
    void getUserPayments_shouldReturnPayments() {
        Payment payment = Payment.builder()
                .id(1L).userId(100L)
                .type(PaymentType.SESSION_BOOKING)
                .amount(900).status(PaymentStatus.SUCCESS)
                .razorpayOrderId("order_123")
                .referenceId(10L)
                .referenceType(ReferenceType.SESSION_BOOKING)
                .build();

        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(payment));

        var result = paymentService.getUserPayments(100L);

        assertEquals(1, result.size());
        assertEquals("SESSION_BOOKING", result.get(0).type());
    }

    @Test
    @DisplayName("getPaymentByOrderId - should return payment for valid owner")
    void getPaymentByOrderId_validOwner_shouldReturn() {
        Payment payment = Payment.builder()
                .id(1L).userId(100L)
                .type(PaymentType.SESSION_BOOKING).amount(900)
                .razorpayOrderId("order_123")
                .status(PaymentStatus.SUCCESS)
                .referenceId(10L)
                .referenceType(ReferenceType.SESSION_BOOKING)
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
                .type(PaymentType.SESSION_BOOKING).amount(900)
                .razorpayOrderId("order_123")
                .status(PaymentStatus.SUCCESS)
                .referenceId(10L).referenceType(ReferenceType.SESSION_BOOKING)
                .build();

        when(paymentRepository.findByRazorpayOrderId("order_123"))
                .thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class, () ->
                paymentService.getPaymentByOrderId(999L, "order_123"));
    }

    @Test
    @DisplayName("hasSuccessfulPayment - should return true when exists")
    void hasSuccessfulPayment_exists_shouldReturnTrue() {
        when(paymentRepository.findByUserIdAndTypeAndStatus(100L, PaymentType.SESSION_BOOKING, PaymentStatus.SUCCESS))
                .thenReturn(List.of(Payment.builder().build()));

        assertTrue(paymentService.hasSuccessfulPayment(100L, PaymentType.SESSION_BOOKING));
    }

    @Test
    @DisplayName("hasSuccessfulPayment - should return false when not exists")
    void hasSuccessfulPayment_notExists_shouldReturnFalse() {
        when(paymentRepository.findByUserIdAndTypeAndStatus(100L, PaymentType.SESSION_BOOKING, PaymentStatus.SUCCESS))
                .thenReturn(List.of());

        assertFalse(paymentService.hasSuccessfulPayment(100L, PaymentType.SESSION_BOOKING));
    }

        @Test
        @DisplayName("createOrder - should reject duplicate active payment for same reference")
        void createOrder_duplicateActivePayment_shouldThrowConflict() {
                CreateOrderRequest request = new CreateOrderRequest(
                                PaymentType.SESSION_BOOKING,
                                10L,
                                ReferenceType.SESSION_BOOKING,
                                900
                );

                when(paymentRepository.findByReferenceIdAndReferenceTypeAndStatusIn(
                                eq(10L),
                                eq(ReferenceType.SESSION_BOOKING),
                                anyList()
                )).thenReturn(List.of(basePayment(PaymentStatus.CREATED, 100L)));

                PaymentException ex = assertThrows(PaymentException.class, () -> paymentService.createOrder(100L, request));

                assertEquals("DUPLICATE_PAYMENT", ex.getErrorCode());
                assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
                verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("verifyPayment - should throw ORDER_NOT_FOUND when order does not exist")
        void verifyPayment_missingOrder_shouldThrowNotFound() {
                VerifyPaymentRequest request = new VerifyPaymentRequest("order_missing", "pay_1", "sig_1");
                when(paymentRepository.findByRazorpayOrderId("order_missing")).thenReturn(Optional.empty());

                PaymentException ex = assertThrows(PaymentException.class, () -> paymentService.verifyPayment(100L, request));

                assertEquals("ORDER_NOT_FOUND", ex.getErrorCode());
                assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
                verify(sagaOrchestrator, never()).executeSaga(any(Payment.class));
        }

        @Test
        @DisplayName("verifyPayment - should reject cross-user verification")
        void verifyPayment_wrongUser_shouldThrowForbidden() {
                VerifyPaymentRequest request = new VerifyPaymentRequest("order_123", "pay_1", "sig_1");
                when(paymentRepository.findByRazorpayOrderId("order_123"))
                                .thenReturn(Optional.of(basePayment(PaymentStatus.CREATED, 200L)));

                PaymentException ex = assertThrows(PaymentException.class, () -> paymentService.verifyPayment(100L, request));

                assertEquals("UNAUTHORIZED_ACCESS", ex.getErrorCode());
                assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
                verify(sagaOrchestrator, never()).executeSaga(any(Payment.class));
        }

        @Test
        @DisplayName("verifyPayment - should return idempotent response for SUCCESS status")
        void verifyPayment_successStatus_shouldReturnCurrentState() {
                VerifyPaymentRequest request = new VerifyPaymentRequest("order_123", "pay_1", "sig_1");
                when(paymentRepository.findByRazorpayOrderId("order_123"))
                                .thenReturn(Optional.of(basePayment(PaymentStatus.SUCCESS, 100L)));

                var response = paymentService.verifyPayment(100L, request);

                assertEquals("SUCCESS", response.status());
                verify(paymentRepository, never()).save(any(Payment.class));
                verify(sagaOrchestrator, never()).executeSaga(any(Payment.class));
        }

        @Test
        @DisplayName("verifyPayment - should fail immediately for already FAILED payment")
        void verifyPayment_failedStatus_shouldThrow() {
                VerifyPaymentRequest request = new VerifyPaymentRequest("order_123", "pay_1", "sig_1");
                when(paymentRepository.findByRazorpayOrderId("order_123"))
                                .thenReturn(Optional.of(basePayment(PaymentStatus.FAILED, 100L)));

                PaymentException ex = assertThrows(PaymentException.class, () -> paymentService.verifyPayment(100L, request));

                assertEquals("PAYMENT_ALREADY_FAILED", ex.getErrorCode());
                verify(paymentRepository, never()).save(any(Payment.class));
                verify(sagaOrchestrator, never()).executeSaga(any(Payment.class));
        }

        @Test
        @DisplayName("verifyPayment - should return current state when saga is already in progress")
        void verifyPayment_successPending_shouldReturnCurrentState() {
                VerifyPaymentRequest request = new VerifyPaymentRequest("order_123", "pay_1", "sig_1");
                when(paymentRepository.findByRazorpayOrderId("order_123"))
                                .thenReturn(Optional.of(basePayment(PaymentStatus.SUCCESS_PENDING, 100L)));

                var response = paymentService.verifyPayment(100L, request);

                assertEquals("SUCCESS_PENDING", response.status());
                verify(paymentRepository, never()).save(any(Payment.class));
                verify(sagaOrchestrator, never()).executeSaga(any(Payment.class));
        }
}

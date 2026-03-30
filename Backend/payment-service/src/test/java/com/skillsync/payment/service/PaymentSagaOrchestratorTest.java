package com.skillsync.payment.service;

import com.skillsync.payment.entity.Payment;
import com.skillsync.payment.entity.SagaState;
import com.skillsync.payment.enums.PaymentStatus;
import com.skillsync.payment.enums.PaymentType;
import com.skillsync.payment.enums.ReferenceType;
import com.skillsync.payment.repository.PaymentRepository;
import com.skillsync.payment.repository.SagaStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PaymentSagaOrchestrator.
 * Verifies:
 * - Saga state transitions (SUCCESS_PENDING → SUCCESS)
 * - Outbox event writing (via OutboxEventService)
 * - Saga state persistence
 * - Compensation flow
 * - Invalid state transition rejection
 */
@ExtendWith(MockitoExtension.class)
class PaymentSagaOrchestratorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SagaStateRepository sagaStateRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private PaymentSagaOrchestrator orchestrator;

    private Payment verifiedPayment;

    @BeforeEach
    void setUp() {
        verifiedPayment = Payment.builder()
                .id(1L)
                .userId(42L)
                .type(PaymentType.SESSION_BOOKING)
                .amount(900)
                .razorpayOrderId("order_test_123")
                .razorpayPaymentId("pay_test_456")
                .status(PaymentStatus.VERIFIED)
                .referenceId(10L)
                .referenceType(ReferenceType.SESSION_BOOKING)
                .build();
    }

    @Test
    @DisplayName("transitionToSuccessPending: VERIFIED → SUCCESS_PENDING + saga state created")
    void transitionToSuccessPending_happyPath() {
        orchestrator.transitionToSuccessPending(verifiedPayment);

        assertEquals(PaymentStatus.SUCCESS_PENDING, verifiedPayment.getStatus());
        verify(paymentRepository).save(verifiedPayment);
        verify(sagaStateRepository).save(any(SagaState.class));
    }

    @Test
    @DisplayName("transitionToSuccessPending: rejects invalid state (already SUCCESS)")
    void transitionToSuccessPending_invalidState() {
        verifiedPayment.setStatus(PaymentStatus.SUCCESS);

        orchestrator.transitionToSuccessPending(verifiedPayment);

        // Should NOT update state or save
        assertEquals(PaymentStatus.SUCCESS, verifiedPayment.getStatus());
        verify(paymentRepository, never()).save(any());
        verify(sagaStateRepository, never()).save(any());
    }

    @Test
    @DisplayName("markPaymentSuccessWithEvents: SUCCESS_PENDING → SUCCESS + outbox events written")
    void markPaymentSuccess_happyPath() {
        verifiedPayment.setStatus(PaymentStatus.SUCCESS_PENDING);
        when(sagaStateRepository.findByPaymentId(1L)).thenReturn(Optional.of(
                SagaState.builder().paymentId(1L).state(PaymentStatus.SUCCESS_PENDING).build()
        ));

        orchestrator.markPaymentSuccessWithEvents(verifiedPayment);

        assertEquals(PaymentStatus.SUCCESS, verifiedPayment.getStatus());
        assertNotNull(verifiedPayment.getCompletedAt());
        verify(paymentRepository).save(verifiedPayment);
        // Should write 2 outbox events: business action + success notification
        verify(outboxEventService, times(2)).saveEvent(anyString(), anyString(), anyString(), any());
        verify(sagaStateRepository).save(any(SagaState.class));
    }

    @Test
    @DisplayName("compensate: marks COMPENSATED + writes outbox event + updates saga state")
    void compensate_writesOutboxAndUpdatesSaga() {
        verifiedPayment.setStatus(PaymentStatus.SUCCESS_PENDING);
        when(sagaStateRepository.findByPaymentId(1L)).thenReturn(Optional.of(
                SagaState.builder().paymentId(1L).state(PaymentStatus.SUCCESS_PENDING).retryCount(0).build()
        ));

        orchestrator.compensate(verifiedPayment, "Business action failed");

        assertEquals(PaymentStatus.COMPENSATED, verifiedPayment.getStatus());
        assertEquals("Business action failed", verifiedPayment.getCompensationReason());
        verify(paymentRepository).save(verifiedPayment);
        verify(outboxEventService).saveEvent(anyString(), eq("payment.compensated.v1"), anyString(), any());
        verify(sagaStateRepository).save(any(SagaState.class));
    }

    @Test
    @DisplayName("executeSaga: full happy path VERIFIED → SUCCESS_PENDING → SUCCESS")
    void executeSaga_fullHappyPath() {
        when(sagaStateRepository.findByPaymentId(1L)).thenReturn(Optional.of(
                SagaState.builder().paymentId(1L).state(PaymentStatus.SUCCESS_PENDING).build()
        ));

        orchestrator.executeSaga(verifiedPayment);

        // After full saga: should be SUCCESS
        assertEquals(PaymentStatus.SUCCESS, verifiedPayment.getStatus());
        verify(paymentRepository, atLeast(2)).save(verifiedPayment);
        verify(outboxEventService, times(2)).saveEvent(anyString(), anyString(), anyString(), any());
    }
}

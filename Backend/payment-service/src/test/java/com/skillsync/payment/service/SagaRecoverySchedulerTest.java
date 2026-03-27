package com.skillsync.payment.service;

import com.skillsync.payment.entity.Payment;
import com.skillsync.payment.entity.SagaState;
import com.skillsync.payment.enums.PaymentStatus;
import com.skillsync.payment.enums.PaymentType;
import com.skillsync.payment.enums.ReferenceType;
import com.skillsync.payment.repository.PaymentRepository;
import com.skillsync.payment.repository.SagaStateRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;


import java.util.Collections;
import java.util.List;
import java.util.Optional;


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SagaRecoveryScheduler:
 * - Stale saga is retried via outbox
 * - Max retries exhausted → compensation
 * - No stale sagas → no action
 */
@ExtendWith(MockitoExtension.class)
class SagaRecoverySchedulerTest {

    @Mock
    private SagaStateRepository sagaStateRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxEventService outboxEventService;

    private SagaRecoveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SagaRecoveryScheduler(sagaStateRepository, paymentRepository,
                outboxEventService, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(scheduler, "timeoutMinutes", 10);
        ReflectionTestUtils.setField(scheduler, "maxRetries", 3);
        scheduler.initMetrics();
    }

    @Test
    @DisplayName("recoverStaleSagas: no stale sagas → no action")
    void recoverStaleSagas_noStale_noAction() {
        when(sagaStateRepository.findStaleSagas(eq(PaymentStatus.SUCCESS_PENDING), any()))
                .thenReturn(Collections.emptyList());

        scheduler.recoverStaleSagas();

        verify(paymentRepository, never()).findById(any());
        verify(outboxEventService, never()).saveEvent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("recoverStaleSagas: stale saga with retries left → retry via outbox")
    void recoverStaleSagas_retriesLeft_retries() {
        SagaState staleSaga = SagaState.builder()
                .id(1L).paymentId(10L).orderId("order_123")
                .state(PaymentStatus.SUCCESS_PENDING).retryCount(1)
                .build();

        Payment payment = Payment.builder()
                .id(10L).userId(42L).type(PaymentType.MENTOR_FEE)
                .amount(900).razorpayOrderId("order_123")
                .status(PaymentStatus.SUCCESS_PENDING)
                .referenceId(5L).referenceType(ReferenceType.MENTOR_ONBOARDING)
                .build();

        when(sagaStateRepository.findStaleSagas(eq(PaymentStatus.SUCCESS_PENDING), any()))
                .thenReturn(List.of(staleSaga));
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        scheduler.recoverStaleSagas();

        // Verify retry event was published via outbox
        verify(outboxEventService).saveEvent(anyString(), eq("payment.business.action.v1"),
                eq("payment.business.action.retry"), any());
        // Verify retry count was incremented
        verify(sagaStateRepository).save(argThat(s -> s.getRetryCount() == 2));
    }

    @Test
    @DisplayName("recoverStaleSagas: max retries exhausted → compensates")
    void recoverStaleSagas_maxRetriesExhausted_compensates() {
        SagaState staleSaga = SagaState.builder()
                .id(1L).paymentId(10L).orderId("order_123")
                .state(PaymentStatus.SUCCESS_PENDING).retryCount(3) // == maxRetries
                .build();

        Payment payment = Payment.builder()
                .id(10L).userId(42L).type(PaymentType.MENTOR_FEE)
                .amount(900).razorpayOrderId("order_123")
                .status(PaymentStatus.SUCCESS_PENDING)
                .referenceId(5L).referenceType(ReferenceType.MENTOR_ONBOARDING)
                .build();

        when(sagaStateRepository.findStaleSagas(eq(PaymentStatus.SUCCESS_PENDING), any()))
                .thenReturn(List.of(staleSaga));
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        scheduler.recoverStaleSagas();

        // Verify payment was COMPENSATED
        verify(paymentRepository).save(argThat(p ->
                p.getStatus() == PaymentStatus.COMPENSATED));
        // Verify compensation event published
        verify(outboxEventService).saveEvent(anyString(), eq("payment.compensated.v1"),
                eq("payment.compensated.recovery"), any());
        // Verify saga state updated to COMPENSATED
        verify(sagaStateRepository).save(argThat(s ->
                s.getState() == PaymentStatus.COMPENSATED));
    }
}

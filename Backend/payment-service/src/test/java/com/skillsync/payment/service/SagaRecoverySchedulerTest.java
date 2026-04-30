package com.skillsync.payment.service;

import com.skillsync.payment.entity.Payment;
import com.skillsync.payment.entity.SagaState;
import com.skillsync.payment.enums.PaymentStatus;
import com.skillsync.payment.enums.PaymentType;
import com.skillsync.payment.repository.PaymentRepository;
import com.skillsync.payment.repository.SagaStateRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SagaRecoveryScheduler Unit Tests")
class SagaRecoverySchedulerTest {

    @Mock private SagaStateRepository sagaStateRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OutboxEventService outboxEventService;
    private MeterRegistry meterRegistry;

    private SagaRecoveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        scheduler = new SagaRecoveryScheduler(sagaStateRepository, paymentRepository, outboxEventService, meterRegistry);
        scheduler.initMetrics();
        ReflectionTestUtils.setField(scheduler, "timeoutMinutes", 10);
        ReflectionTestUtils.setField(scheduler, "maxRetries", 3);
    }

    @Test
    @DisplayName("recoverStaleSagas: Retries pending saga")
    void recover_Retries() {
        SagaState saga = SagaState.builder()
                .paymentId(1L)
                .state(PaymentStatus.SUCCESS_PENDING)
                .retryCount(0)
                .build();
        
        Payment payment = Payment.builder()
                .id(1L)
                .userId(1L)
                .type(PaymentType.SESSION_BOOKING)
                .amount(1000)
                .razorpayOrderId("order1")
                .build();

        when(sagaStateRepository.findStaleSagas(any(), any())).thenReturn(List.of(saga));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        scheduler.recoverStaleSagas();

        verify(outboxEventService).saveEvent(any(), any(), any(), any());
        verify(sagaStateRepository).save(any());
    }

    @Test
    @DisplayName("recoverStaleSagas: Compensates if max retries reached")
    void recover_Compensates() {
        SagaState saga = SagaState.builder()
                .paymentId(1L)
                .state(PaymentStatus.SUCCESS_PENDING)
                .retryCount(5)
                .build();
        
        Payment payment = Payment.builder()
                .id(1L)
                .userId(1L)
                .type(PaymentType.SESSION_BOOKING)
                .amount(1000)
                .razorpayOrderId("order1")
                .build();

        when(sagaStateRepository.findStaleSagas(any(), any())).thenReturn(List.of(saga));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        scheduler.recoverStaleSagas();

        verify(sagaStateRepository).save(any());
        verify(paymentRepository, atLeastOnce()).save(any());
    }
}

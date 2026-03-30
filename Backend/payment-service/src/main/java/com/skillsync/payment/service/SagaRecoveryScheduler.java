package com.skillsync.payment.service;

import com.skillsync.payment.config.RabbitMQConfig;
import com.skillsync.payment.entity.Payment;
import com.skillsync.payment.entity.SagaState;
import com.skillsync.payment.enums.PaymentStatus;
import com.skillsync.payment.event.PaymentCompletedEvent;
import com.skillsync.payment.repository.PaymentRepository;
import com.skillsync.payment.repository.SagaStateRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Saga recovery scheduler that detects and resolves stuck sagas.
 *
 * <h3>Problem:</h3>
 * A saga may get stuck in {@code SUCCESS_PENDING} if the publisher crashes
 * after writing the saga state but before completing the downstream actions.
 *
 * <h3>Recovery Strategy:</h3>
 * <ol>
 *   <li>Polls for sagas in SUCCESS_PENDING older than the configured timeout</li>
 *   <li>For each stale saga, re-publishes the business.action event via outbox</li>
 *   <li>Increments retry_count — stops after max retries and compensates</li>
 * </ol>
 */
@Component
@Slf4j
public class SagaRecoveryScheduler {

    private final SagaStateRepository sagaStateRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxEventService outboxEventService;
    private final MeterRegistry meterRegistry;

    @Value("${saga.recovery.timeout-minutes:10}")
    private int timeoutMinutes;

    @Value("${saga.recovery.max-retries:3}")
    private int maxRetries;

    private Counter sagaRecoveryCounter;
    private Counter sagaCompensationCounter;

    public SagaRecoveryScheduler(SagaStateRepository sagaStateRepository,
                                  PaymentRepository paymentRepository,
                                  OutboxEventService outboxEventService,
                                  MeterRegistry meterRegistry) {
        this.sagaStateRepository = sagaStateRepository;
        this.paymentRepository = paymentRepository;
        this.outboxEventService = outboxEventService;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void initMetrics() {
        sagaRecoveryCounter = Counter.builder("saga.recovery.attempts")
                .description("Saga recovery retry attempts")
                .register(meterRegistry);
        sagaCompensationCounter = Counter.builder("saga.recovery.compensations")
                .description("Sagas compensated due to max retries exhausted")
                .register(meterRegistry);
    }

    /**
     * Runs at configured interval (default: every 5 minutes).
     * Detects stuck sagas and either retries or compensates.
     */
    @Scheduled(fixedDelayString = "${saga.recovery.interval-ms:300000}")
    @Transactional
    public void recoverStaleSagas() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

        List<SagaState> staleSagas = sagaStateRepository.findStaleSagas(
                PaymentStatus.SUCCESS_PENDING, threshold);

        if (staleSagas.isEmpty()) {
            return;
        }

        log.warn("[SAGA-RECOVERY] Found {} stale sagas (older than {} min)",
                staleSagas.size(), timeoutMinutes);

        for (SagaState saga : staleSagas) {
            try {
                recoverSaga(saga);
            } catch (Exception e) {
                log.error("[SAGA-RECOVERY] Recovery failed for saga paymentId={}, orderId={}: {}",
                        saga.getPaymentId(), saga.getOrderId(), e.getMessage(), e);
            }
        }
    }

    private void recoverSaga(SagaState saga) {
        Payment payment = paymentRepository.findById(saga.getPaymentId()).orElse(null);
        if (payment == null) {
            log.error("[SAGA-RECOVERY] Payment not found for saga paymentId={}", saga.getPaymentId());
            return;
        }

        if (saga.getRetryCount() >= maxRetries) {
            // Max retries exhausted — compensate
            compensateSaga(saga, payment);
            return;
        }

        // Retry: re-publish business action event via outbox
        log.info("[SAGA-RECOVERY] Retrying stale saga: paymentId={}, orderId={}, attempt={}/{}",
                saga.getPaymentId(), saga.getOrderId(), saga.getRetryCount() + 1, maxRetries);

        PaymentCompletedEvent retryEvent = new PaymentCompletedEvent(
                UUID.randomUUID().toString(),
                "1",
                LocalDateTime.now().toString(),
                payment.getUserId(),
                payment.getRazorpayOrderId(),
                payment.getType().name(),
                PaymentStatus.SUCCESS_PENDING.name(),
                payment.getAmount(),
                payment.getReferenceId(),
                payment.getReferenceType() != null ? payment.getReferenceType().name() : null,
                null
        );

        outboxEventService.saveEvent(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                "payment.business.action.v1",
                "payment.business.action.retry",
                retryEvent
        );

        saga.setRetryCount(saga.getRetryCount() + 1);
        saga.setLastError("Saga recovery retry #" + saga.getRetryCount());
        sagaStateRepository.save(saga);

        sagaRecoveryCounter.increment();
        log.info("[SAGA-RECOVERY] Retry event published for paymentId={}, orderId={}",
                saga.getPaymentId(), saga.getOrderId());
    }

    private void compensateSaga(SagaState saga, Payment payment) {
        log.warn("[SAGA-RECOVERY] Max retries exhausted for paymentId={}, orderId={}. Compensating.",
                saga.getPaymentId(), saga.getOrderId());

        // Mark payment as COMPENSATED
        payment.setStatus(PaymentStatus.COMPENSATED);
        payment.setCompensationReason("Saga recovery: max retries (" + maxRetries + ") exhausted");
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Update saga state
        saga.setState(PaymentStatus.COMPENSATED);
        saga.setLastError("Max retries exhausted — auto-compensated by recovery scheduler");
        sagaStateRepository.save(saga);

        // Publish compensation event via outbox
        PaymentCompletedEvent compensatedEvent = new PaymentCompletedEvent(
                UUID.randomUUID().toString(),
                "1",
                LocalDateTime.now().toString(),
                payment.getUserId(),
                payment.getRazorpayOrderId(),
                payment.getType().name(),
                PaymentStatus.COMPENSATED.name(),
                payment.getAmount(),
                payment.getReferenceId(),
                payment.getReferenceType() != null ? payment.getReferenceType().name() : null,
                "Saga recovery: max retries exhausted"
        );

        outboxEventService.saveEvent(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                "payment.compensated.v1",
                "payment.compensated.recovery",
                compensatedEvent
        );

        sagaCompensationCounter.increment();
        log.warn("[SAGA-RECOVERY] Payment compensated: orderId={}, userId={}",
                payment.getRazorpayOrderId(), payment.getUserId());
    }
}

package com.skillsync.payment.service;

import com.skillsync.payment.config.RabbitMQConfig;
import com.skillsync.payment.entity.Payment;
import com.skillsync.payment.entity.SagaState;
import com.skillsync.payment.enums.PaymentStatus;
import com.skillsync.payment.event.PaymentCompletedEvent;
import com.skillsync.payment.repository.PaymentRepository;
import com.skillsync.payment.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestration-based Saga for post-payment business actions.
 *
 * <h3>Production-Grade Features:</h3>
 * <ul>
 *   <li><b>Outbox Pattern:</b> Events are written to the outbox table (same TX as state change),
 *       then published by the background {@link OutboxPublisher}</li>
 *   <li><b>Saga State Persistence:</b> Every state transition is persisted in the {@code saga_state}
 *       table for recovery after restart</li>
 *   <li><b>Event Versioning:</b> All events include eventId, version, and timestamp headers</li>
 *   <li><b>Idempotent State Transitions:</b> Invalid state transitions are rejected</li>
 * </ul>
 *
 * <h3>Event-Driven Flow:</h3>
 * <ol>
 *   <li>Mark payment as SUCCESS_PENDING + create saga state</li>
 *   <li>Write {@code payment.business.action.v1} event to outbox (same TX)</li>
 *   <li>Mark payment as SUCCESS + write {@code payment.success.v1} to outbox</li>
 *   <li>OutboxPublisher polls and publishes to RabbitMQ asynchronously</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaOrchestrator {

    private final PaymentRepository paymentRepository;
    private final SagaStateRepository sagaStateRepository;
    private final OutboxEventService outboxEventService;

    // ─────────────────────────────────────────────
    //  SAGA ENTRYPOINT
    // ─────────────────────────────────────────────

    /**
     * Executes the full saga for a verified payment.
     * This must be called AFTER payment verification is committed.
     *
     * @param payment the verified payment (status = VERIFIED)
     */
    public void executeSaga(Payment payment) {
        log.info("[SAGA] Starting orchestration for orderId={}, type={}, userId={}, referenceId={}, referenceType={}",
                payment.getRazorpayOrderId(), payment.getType(), payment.getUserId(),
                payment.getReferenceId(), payment.getReferenceType());

        // STEP 1: Transition to SUCCESS_PENDING + create saga state
        transitionToSuccessPending(payment);

        try {
            // STEP 2: Write business action event to outbox (same TX as SUCCESS mark)
            // STEP 3: Mark as SUCCESS
            markPaymentSuccessWithEvents(payment);

            log.info("[SAGA] Completed successfully for orderId={}, userId={}",
                    payment.getRazorpayOrderId(), payment.getUserId());

        } catch (Exception e) {
            // STEP 3b: Failure — trigger compensation
            log.error("[SAGA] Saga failed for orderId={}, type={}, userId={}. Triggering compensation.",
                    payment.getRazorpayOrderId(), payment.getType(), payment.getUserId(), e);

            compensate(payment, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  SAGA STEPS
    // ─────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void transitionToSuccessPending(Payment payment) {
        // State validation: only VERIFIED → SUCCESS_PENDING is valid
        if (payment.getStatus() != PaymentStatus.VERIFIED) {
            log.warn("[SAGA] Invalid state transition: cannot go from {} to SUCCESS_PENDING for orderId={}",
                    payment.getStatus(), payment.getRazorpayOrderId());
            return;
        }

        payment.setStatus(PaymentStatus.SUCCESS_PENDING);
        paymentRepository.save(payment);

        // Persist saga state
        SagaState sagaState = SagaState.builder()
                .paymentId(payment.getId())
                .orderId(payment.getRazorpayOrderId())
                .state(PaymentStatus.SUCCESS_PENDING)
                .retryCount(0)
                .build();
        sagaStateRepository.save(sagaState);

        log.info("[SAGA] Payment transitioned to SUCCESS_PENDING: orderId={}", payment.getRazorpayOrderId());
    }

    /**
     * Marks payment as SUCCESS and writes both business action + success notification
     * events to the outbox in a single transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPaymentSuccessWithEvents(Payment payment) {
        // State validation: only SUCCESS_PENDING → SUCCESS is valid
        if (payment.getStatus() != PaymentStatus.SUCCESS_PENDING) {
            log.warn("[SAGA] Invalid state transition: cannot go from {} to SUCCESS for orderId={}",
                    payment.getStatus(), payment.getRazorpayOrderId());
            return;
        }

        // Build the business action event
        PaymentCompletedEvent businessEvent = buildEvent(payment, PaymentStatus.SUCCESS_PENDING.name(), null);

        // Write business action event to outbox (same TX as state change)
        outboxEventService.saveEvent(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                "payment.business.action.v1",
                "payment.business.action",
                businessEvent
        );

        // Mark payment as SUCCESS
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Build success notification event
        PaymentCompletedEvent successEvent = buildEvent(payment, PaymentStatus.SUCCESS.name(), null);

        // Write success notification event to outbox (same TX)
        outboxEventService.saveEvent(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                "payment.success.v1",
                "payment.success",
                successEvent
        );

        // Update saga state
        sagaStateRepository.findByPaymentId(payment.getId()).ifPresent(saga -> {
            saga.setState(PaymentStatus.SUCCESS);
            sagaStateRepository.save(saga);
        });

        log.info("[SAGA] Payment marked as SUCCESS: orderId={}, userId={}",
                payment.getRazorpayOrderId(), payment.getUserId());
    }

    // ─────────────────────────────────────────────
    //  COMPENSATION (Rollback Strategy)
    // ─────────────────────────────────────────────

    /**
     * Compensation logic: marks payment as COMPENSATED and writes
     * notification event to the outbox.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(Payment payment, String failureReason) {
        log.warn("[COMPENSATION] Starting compensation for orderId={}, type={}, userId={}, reason={}",
                payment.getRazorpayOrderId(), payment.getType(), payment.getUserId(), failureReason);

        // Mark payment as COMPENSATED
        payment.setStatus(PaymentStatus.COMPENSATED);
        payment.setCompensationReason(failureReason);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Write compensation notification to outbox (same TX)
        PaymentCompletedEvent compensatedEvent = buildEvent(payment, PaymentStatus.COMPENSATED.name(), failureReason);
        outboxEventService.saveEvent(
                RabbitMQConfig.PAYMENT_EXCHANGE,
                "payment.compensated.v1",
                "payment.compensated",
                compensatedEvent
        );

        // Update saga state
        sagaStateRepository.findByPaymentId(payment.getId()).ifPresent(saga -> {
            saga.setState(PaymentStatus.COMPENSATED);
            saga.setLastError(failureReason);
            saga.setRetryCount(saga.getRetryCount() + 1);
            sagaStateRepository.save(saga);
        });

        log.warn("[COMPENSATION] Payment marked as COMPENSATED: orderId={}, userId={}",
                payment.getRazorpayOrderId(), payment.getUserId());
    }

    // ─────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────

    private PaymentCompletedEvent buildEvent(Payment payment, String status, String compensationReason) {
        return new PaymentCompletedEvent(
                UUID.randomUUID().toString(),
                "1",
                LocalDateTime.now().toString(),
                payment.getUserId(),
                payment.getRazorpayOrderId(),
                payment.getType().name(),
                status,
                payment.getAmount(),
                payment.getReferenceId(),
                payment.getReferenceType() != null ? payment.getReferenceType().name() : null,
                compensationReason
        );
    }
}

package com.skillsync.user.service;

import com.skillsync.cache.CacheService;
import com.skillsync.user.config.RabbitMQConfig;
import com.skillsync.user.entity.Payment;
import com.skillsync.user.enums.PaymentStatus;
import com.skillsync.user.enums.PaymentType;
import com.skillsync.user.enums.ReferenceType;
import com.skillsync.user.event.PaymentCompletedEvent;
import com.skillsync.user.repository.PaymentRepository;
import com.skillsync.user.service.command.MentorCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Orchestration-based Saga for post-payment business actions.
 *
 * <p>This component isolates the saga logic from PaymentService to keep
 * concerns separated and make it easy to extract into a standalone Payment
 * Service (or event-driven orchestrator) in the future.</p>
 *
 * <h3>Flow:</h3>
 * <ol>
 *   <li>Mark payment as SUCCESS_PENDING (saga started)</li>
 *   <li>Execute the appropriate business action based on payment type</li>
 *   <li>On success → mark payment as SUCCESS + publish notification event</li>
 *   <li>On failure → trigger compensation + publish notification event</li>
 * </ol>
 *
 * <h3>Design Note (Interview):</h3>
 * <p>In a distributed system, this orchestrator would publish events to
 * Kafka/RabbitMQ and wait for responses. The current implementation simulates
 * the same pattern within a single service boundary.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaOrchestrator {

    private final PaymentRepository paymentRepository;
    private final MentorCommandService mentorCommandService;
    private final RabbitTemplate rabbitTemplate;
    private final CacheService cacheService;

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

        // STEP 1: Transition to SUCCESS_PENDING
        transitionToSuccessPending(payment);

        // STEP 2: Execute business action
        try {
            executeBusinessAction(payment);

            // STEP 3a: Success — mark as SUCCESS
            markPaymentSuccess(payment);

            // Publish payment success notification
            publishPaymentEvent(payment, "payment.success");

            log.info("[SAGA] Completed successfully for orderId={}, userId={}",
                    payment.getRazorpayOrderId(), payment.getUserId());

        } catch (Exception e) {
            // STEP 3b: Failure — trigger compensation
            log.error("[SAGA] Business action failed for orderId={}, type={}, userId={}. Triggering compensation.",
                    payment.getRazorpayOrderId(), payment.getType(), payment.getUserId(), e);

            compensate(payment, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  SAGA STEPS
    // ─────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void transitionToSuccessPending(Payment payment) {
        payment.setStatus(PaymentStatus.SUCCESS_PENDING);
        paymentRepository.save(payment);
        log.info("[SAGA] Payment transitioned to SUCCESS_PENDING: orderId={}", payment.getRazorpayOrderId());
    }

    /**
     * Executes the appropriate business action based on payment type.
     * Each business action is a modular method for easy extension.
     */
    private void executeBusinessAction(Payment payment) {
        switch (payment.getType()) {
            case MENTOR_FEE -> executeMentorOnboarding(payment);
            case SESSION_BOOKING -> executeSessionBooking(payment);
            default -> {
                log.warn("[SAGA] Unknown payment type: {}", payment.getType());
                throw new IllegalStateException("Unsupported payment type: " + payment.getType());
            }
        }
    }

    // ─────────────────────────────────────────────
    //  BUSINESS ACTIONS (Modular)
    // ─────────────────────────────────────────────

    /**
     * Mentor onboarding: approve mentor profile and update user role.
     */
    private void executeMentorOnboarding(Payment payment) {
        log.info("[SAGA:MENTOR] Triggering mentor onboarding for userId={}, referenceId={}",
                payment.getUserId(), payment.getReferenceId());

        if (payment.getReferenceId() == null) {
            throw new IllegalStateException("referenceId (mentorProfileId) is required for MENTOR_FEE payments");
        }

        // referenceId = mentorProfileId
        mentorCommandService.approveMentor(payment.getReferenceId());

        log.info("[SAGA:MENTOR] Mentor onboarding completed: mentorId={}, userId={}",
                payment.getReferenceId(), payment.getUserId());
    }

    /**
     * Session booking: handled by session-service via Feign or event.
     * Currently the frontend calls session API after receiving SUCCESS response.
     */
    private void executeSessionBooking(Payment payment) {
        log.info("[SAGA:SESSION] Session booking payment processed for userId={}, referenceId={}",
                payment.getUserId(), payment.getReferenceId());

        if (payment.getReferenceId() == null) {
            throw new IllegalStateException("referenceId (sessionRequestId) is required for SESSION_BOOKING payments");
        }

        // Session creation is handled externally by session-service.
        // The payment SUCCESS status acts as a gate for session-service to verify before creating sessions.
        // In a future distributed saga, this would publish an event to session-service.
        log.info("[SAGA:SESSION] Payment gate set for sessionRequest={}. Session-service can proceed.",
                payment.getReferenceId());
    }

    // ─────────────────────────────────────────────
    //  COMPENSATION (Rollback Strategy)
    // ─────────────────────────────────────────────

    /**
     * Compensation logic: reverses any partially applied business effects.
     * NOTE: Razorpay payment CANNOT be reversed — compensation only affects internal state.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(Payment payment, String failureReason) {
        log.warn("[COMPENSATION] Starting compensation for orderId={}, type={}, userId={}, reason={}",
                payment.getRazorpayOrderId(), payment.getType(), payment.getUserId(), failureReason);

        try {
            switch (payment.getType()) {
                case MENTOR_FEE -> compensateMentorOnboarding(payment);
                case SESSION_BOOKING -> compensateSessionBooking(payment);
            }
        } catch (Exception compensationEx) {
            log.error("[COMPENSATION] CRITICAL — Compensation itself failed for orderId={}. " +
                            "Manual intervention required! Original failure: {}",
                    payment.getRazorpayOrderId(), failureReason, compensationEx);
        }

        // Mark payment as COMPENSATED regardless of compensation success/failure
        payment.setStatus(PaymentStatus.COMPENSATED);
        payment.setCompensationReason(failureReason);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Publish compensation notification
        publishPaymentEvent(payment, "payment.compensated");

        log.warn("[COMPENSATION] Payment marked as COMPENSATED: orderId={}, userId={}",
                payment.getRazorpayOrderId(), payment.getUserId());
    }

    /**
     * Revert mentor approval: set status back to PENDING and revert role.
     */
    private void compensateMentorOnboarding(Payment payment) {
        log.warn("[COMPENSATION:MENTOR] Reverting mentor approval for referenceId={}, userId={}",
                payment.getReferenceId(), payment.getUserId());

        try {
            mentorCommandService.revertMentorApproval(payment.getReferenceId());
            log.info("[COMPENSATION:MENTOR] Successfully reverted mentor approval for mentorId={}",
                    payment.getReferenceId());
        } catch (Exception e) {
            log.error("[COMPENSATION:MENTOR] Failed to revert mentor approval for mentorId={}",
                    payment.getReferenceId(), e);
            throw e;
        }
    }

    /**
     * Revert session booking: log for manual cleanup.
     * Session creation is external, so we mark for recovery.
     */
    private void compensateSessionBooking(Payment payment) {
        log.warn("[COMPENSATION:SESSION] Session booking compensation triggered for referenceId={}, userId={}. " +
                        "Session-service should check payment status before creating sessions.",
                payment.getReferenceId(), payment.getUserId());
        // In a distributed system, this would publish a compensation event to session-service
    }

    // ─────────────────────────────────────────────
    //  STATE TRANSITIONS
    // ─────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPaymentSuccess(Payment payment) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        log.info("[SAGA] Payment marked as SUCCESS: orderId={}, userId={}",
                payment.getRazorpayOrderId(), payment.getUserId());
    }

    // ─────────────────────────────────────────────
    //  NOTIFICATION EVENT PUBLISHING
    // ─────────────────────────────────────────────

    /**
     * Publishes a payment lifecycle event to RabbitMQ for the notification service.
     *
     * @param payment    the payment entity
     * @param routingKey the routing key (payment.success, payment.failed, payment.compensated)
     */
    private void publishPaymentEvent(Payment payment, String routingKey) {
        try {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    payment.getUserId(),
                    payment.getRazorpayOrderId(),
                    payment.getType().name(),
                    payment.getStatus().name(),
                    payment.getAmount(),
                    payment.getReferenceId(),
                    payment.getReferenceType() != null ? payment.getReferenceType().name() : null,
                    payment.getCompensationReason()
            );

            rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, routingKey, event);
            log.info("[NOTIFICATION] Published {} event for orderId={}, userId={}",
                    routingKey, payment.getRazorpayOrderId(), payment.getUserId());
        } catch (Exception e) {
            // Notification failure should NOT affect payment flow
            log.error("[NOTIFICATION] Failed to publish {} event for orderId={}. " +
                            "Notification will not be sent, but payment state is correct.",
                    routingKey, payment.getRazorpayOrderId(), e);
        }
    }
}

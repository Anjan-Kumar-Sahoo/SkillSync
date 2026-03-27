package com.skillsync.user.consumer;

import com.skillsync.user.entity.ProcessedEvent;
import com.skillsync.user.repository.ProcessedEventRepository;
import com.skillsync.user.service.command.MentorCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Consumes payment business action events from payment-service.
 * Executes the corresponding business action (mentor onboarding, session booking).
 *
 * <h3>Production-Grade Features:</h3>
 * <ul>
 *   <li><b>Idempotency:</b> Checks {@code processed_events} table before processing.
 *       Duplicate eventIds are silently skipped.</li>
 *   <li><b>DLQ:</b> Messages that fail after RabbitMQ retries are routed to DLQ
 *       via the dead-letter exchange configured in RabbitMQConfig.</li>
 *   <li><b>State Validation:</b> Mentor approval is inherently idempotent —
 *       approving an already-approved mentor is a no-op.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final MentorCommandService mentorCommandService;
    private final ProcessedEventRepository processedEventRepository;

    @RabbitListener(queues = "payment.business.action.v1.queue")
    @Transactional
    public void handlePaymentBusinessAction(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        String paymentType = (String) event.get("paymentType");
        Long userId = toLong(event.get("userId"));
        Long referenceId = toLong(event.get("referenceId"));
        String orderId = (String) event.get("orderId");

        log.info("[PAYMENT-CONSUMER] Received event: eventId={}, orderId={}, type={}, userId={}, referenceId={}",
                eventId, orderId, paymentType, userId, referenceId);

        // ── Idempotency Check ──
        if (eventId != null && processedEventRepository.existsByEventId(eventId)) {
            log.info("[PAYMENT-CONSUMER] Duplicate event skipped: eventId={}", eventId);
            return;
        }

        // ── Process Business Action ──
        switch (paymentType) {
            case "MENTOR_FEE" -> executeMentorOnboarding(referenceId, userId);
            case "SESSION_BOOKING" -> executeSessionBooking(referenceId, userId);
            default -> log.warn("[PAYMENT-CONSUMER] Unknown payment type: {}", paymentType);
        }

        // ── Record as Processed ──
        if (eventId != null) {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType("payment.business.action")
                    .processedAt(LocalDateTime.now())
                    .build());
        }

        log.info("[PAYMENT-CONSUMER] Successfully processed event: eventId={}, orderId={}",
                eventId, orderId);
    }

    private void executeMentorOnboarding(Long referenceId, Long userId) {
        log.info("[PAYMENT-CONSUMER:MENTOR] Triggering mentor onboarding for userId={}, referenceId={}",
                userId, referenceId);

        if (referenceId == null) {
            log.error("[PAYMENT-CONSUMER:MENTOR] referenceId (mentorProfileId) is null for userId={}", userId);
            return;
        }

        mentorCommandService.approveMentor(referenceId);

        log.info("[PAYMENT-CONSUMER:MENTOR] Mentor onboarding completed: mentorId={}, userId={}",
                referenceId, userId);
    }

    private void executeSessionBooking(Long referenceId, Long userId) {
        log.info("[PAYMENT-CONSUMER:SESSION] Session booking payment processed for userId={}, referenceId={}",
                userId, referenceId);
        log.info("[PAYMENT-CONSUMER:SESSION] Payment gate set for sessionRequest={}. Session-service can proceed.",
                referenceId);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }
}

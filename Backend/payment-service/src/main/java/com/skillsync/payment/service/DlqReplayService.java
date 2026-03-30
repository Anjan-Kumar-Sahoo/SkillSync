package com.skillsync.payment.service;

import com.skillsync.payment.config.RabbitMQConfig;
import com.skillsync.payment.entity.FailedEvent;
import com.skillsync.payment.repository.FailedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DLQ Replay service for safely re-publishing failed events.
 *
 * <h3>Safety Guarantees:</h3>
 * <ul>
 *   <li>Replay preserves the original eventId → consumers enforce idempotency</li>
 *   <li>Events already in REPLAYED/SKIPPED state are rejected</li>
 *   <li>The original routing key is used for re-publishing</li>
 * </ul>
 */
@Service
@Slf4j
public class DlqReplayService {

    private final FailedEventRepository failedEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;
    private Counter replaySuccessCounter;
    private Counter replaySkippedCounter;

    public DlqReplayService(FailedEventRepository failedEventRepository,
                            RabbitTemplate rabbitTemplate,
                            MeterRegistry meterRegistry) {
        this.failedEventRepository = failedEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void initMetrics() {
        replaySuccessCounter = Counter.builder("dlq.replay.success")
                .description("DLQ events successfully replayed")
                .register(meterRegistry);
        replaySkippedCounter = Counter.builder("dlq.replay.skipped")
                .description("DLQ events skipped during replay (already processed)")
                .register(meterRegistry);
    }

    /**
     * Replays a single failed event by eventId.
     * Re-publishes to the original exchange/routing key with the ORIGINAL eventId
     * so consumer-side idempotency will catch duplicates.
     *
     * @return a status map indicating success or skip reason
     */
    @Transactional
    public Map<String, String> replayEvent(String eventId) {
        FailedEvent failedEvent = failedEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new RuntimeException("Failed event not found: " + eventId));

        // Prevent re-replay
        if (failedEvent.getReplayStatus() != FailedEvent.ReplayStatus.PENDING_REVIEW) {
            String msg = "Event already " + failedEvent.getReplayStatus() + ": " + eventId;
            log.warn("[DLQ-REPLAY] {}", msg);
            replaySkippedCounter.increment();
            return Map.of("status", "SKIPPED", "reason", msg);
        }

        try {
            // Re-publish with original eventId (consumers will dedup if already processed)
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PAYMENT_EXCHANGE,
                    failedEvent.getRoutingKey(),
                    failedEvent.getPayload(),
                    message -> {
                        message.getMessageProperties().setHeader("x-event-id", failedEvent.getEventId());
                        message.getMessageProperties().setHeader("x-event-type", "dlq-replay");
                        message.getMessageProperties().setHeader("x-event-version", "1");
                        message.getMessageProperties().setHeader("x-replay", "true");
                        return message;
                    }
            );

            failedEvent.setReplayStatus(FailedEvent.ReplayStatus.REPLAYED);
            failedEvent.setReplayedAt(LocalDateTime.now());
            failedEventRepository.save(failedEvent);

            replaySuccessCounter.increment();
            log.info("[DLQ-REPLAY] Event replayed: eventId={}, routingKey={}",
                    eventId, failedEvent.getRoutingKey());

            return Map.of("status", "REPLAYED", "eventId", eventId);

        } catch (Exception e) {
            log.error("[DLQ-REPLAY] Replay failed for eventId={}: {}", eventId, e.getMessage(), e);
            return Map.of("status", "FAILED", "reason", e.getMessage());
        }
    }

    /**
     * Marks a failed event as SKIPPED (intentionally not replaying).
     */
    @Transactional
    public Map<String, String> skipEvent(String eventId) {
        FailedEvent failedEvent = failedEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new RuntimeException("Failed event not found: " + eventId));

        failedEvent.setReplayStatus(FailedEvent.ReplayStatus.SKIPPED);
        failedEventRepository.save(failedEvent);

        log.info("[DLQ-REPLAY] Event skipped: eventId={}", eventId);
        return Map.of("status", "SKIPPED", "eventId", eventId);
    }

    /**
     * Lists all failed events pending review.
     */
    public List<FailedEvent> getPendingReview() {
        return failedEventRepository.findByReplayStatus(FailedEvent.ReplayStatus.PENDING_REVIEW);
    }
}

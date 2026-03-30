package com.skillsync.payment.consumer;

import com.skillsync.payment.config.RabbitMQConfig;
import com.skillsync.payment.entity.FailedEvent;
import com.skillsync.payment.repository.FailedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Dead Letter Queue consumer that persists failed events to the {@code failed_events}
 * table for admin review and safe replay.
 *
 * <h3>DLQ Strategy:</h3>
 * <ul>
 *   <li>Messages that fail consumer processing after retries are routed to DLQ</li>
 *   <li>This consumer extracts eventId, payload, and error details</li>
 *   <li>Persists to {@code failed_events} table with status PENDING_REVIEW</li>
 *   <li>Admin can replay via the internal replay endpoint</li>
 * </ul>
 */
@Component
@Slf4j
public class DlqConsumer {

    private final FailedEventRepository failedEventRepository;
    private final MeterRegistry meterRegistry;
    private Counter dlqCounter;

    public DlqConsumer(FailedEventRepository failedEventRepository, MeterRegistry meterRegistry) {
        this.failedEventRepository = failedEventRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void initMetrics() {
        dlqCounter = Counter.builder("dlq.events.received")
                .description("Total DLQ events received for review")
                .register(meterRegistry);
    }

    @RabbitListener(queues = RabbitMQConfig.DLQ_BUSINESS_ACTION)
    public void handleBusinessActionDlq(Message message) {
        persistFailedEvent(message, RabbitMQConfig.DLQ_BUSINESS_ACTION, "payment.business.action.v1");
    }

    @RabbitListener(queues = RabbitMQConfig.DLQ_SUCCESS)
    public void handleSuccessDlq(Message message) {
        persistFailedEvent(message, RabbitMQConfig.DLQ_SUCCESS, "payment.success.v1");
    }

    @RabbitListener(queues = RabbitMQConfig.DLQ_FAILED)
    public void handleFailedDlq(Message message) {
        persistFailedEvent(message, RabbitMQConfig.DLQ_FAILED, "payment.failed.v1");
    }

    private void persistFailedEvent(Message message, String sourceQueue, String routingKey) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        String eventId = extractHeader(message, "x-event-id");
        String errorReason = extractDeathReason(message);

        log.error("[DLQ] Failed event persisted for review: sourceQueue={}, eventId={}, routingKey={}, error={}",
                sourceQueue, eventId, routingKey, errorReason);

        // Prevent duplicate DLQ entries
        if (eventId != null && failedEventRepository.existsByEventId(eventId)) {
            log.warn("[DLQ] Duplicate DLQ entry skipped: eventId={}", eventId);
            return;
        }

        failedEventRepository.save(FailedEvent.builder()
                .eventId(eventId != null ? eventId : "unknown-" + System.currentTimeMillis())
                .sourceQueue(sourceQueue)
                .routingKey(routingKey)
                .payload(payload)
                .errorReason(errorReason != null ? errorReason : "Unknown failure")
                .replayStatus(FailedEvent.ReplayStatus.PENDING_REVIEW)
                .failedAt(LocalDateTime.now())
                .build());

        dlqCounter.increment();
    }

    private String extractHeader(Message message, String header) {
        Object value = message.getMessageProperties().getHeader(header);
        return value != null ? value.toString() : null;
    }

    private String extractDeathReason(Message message) {
        // RabbitMQ sets x-death headers on DLQ messages with failure details
        var xDeath = message.getMessageProperties().getHeader("x-death");
        if (xDeath instanceof java.util.List<?> deathList && !deathList.isEmpty()) {
            return deathList.get(0).toString();
        }
        return null;
    }
}

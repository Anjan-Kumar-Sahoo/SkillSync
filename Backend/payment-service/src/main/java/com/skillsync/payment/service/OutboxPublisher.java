package com.skillsync.payment.service;

import com.skillsync.payment.entity.OutboxEvent;
import com.skillsync.payment.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Production-grade Outbox Publisher with:
 * <ul>
 *   <li><b>Publisher Confirms:</b> Waits for broker ACK before marking SENT.
 *       On NACK/timeout → marks FAILED for retry.</li>
 *   <li><b>Distributed Locking:</b> Uses {@code FOR UPDATE SKIP LOCKED} so
 *       multiple instances never process the same event.</li>
 *   <li><b>PROCESSING status:</b> PENDING → PROCESSING → SENT/FAILED.
 *       Stale PROCESSING rows (crashed publishers) are recovered automatically.</li>
 *   <li><b>Metrics:</b> Counters for publish success/failure via Micrometer.</li>
 * </ul>
 */
@Component
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${outbox.publisher.confirm-timeout-ms:5000}")
    private long confirmTimeoutMs;

    @Value("${outbox.publisher.batch-size:50}")
    private int batchSize;

    @Value("${outbox.publisher.max-retries:5}")
    private int maxRetries;

    private Counter publishSuccessCounter;
    private Counter publishFailureCounter;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           RabbitTemplate rabbitTemplate,
                           MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void initMetrics() {
        publishSuccessCounter = Counter.builder("outbox.publish.success")
                .description("Outbox events successfully published to RabbitMQ")
                .register(meterRegistry);
        publishFailureCounter = Counter.builder("outbox.publish.failure")
                .description("Outbox events that failed to publish")
                .register(meterRegistry);
    }

    // ─────────────────────────────────────────────
    //  SCHEDULED: Publish PENDING events
    // ─────────────────────────────────────────────

    /**
     * Claims and publishes PENDING outbox events every 2 seconds.
     * Uses FOR UPDATE SKIP LOCKED — safe for multiple instances.
     */
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findAndLockPendingEvents(batchSize);
        for (OutboxEvent event : pendingEvents) {
            claimAndPublish(event);
        }

        // Also retry FAILED events (up to maxRetries)
        List<OutboxEvent> failedEvents = outboxEventRepository.findAndLockFailedEventsForRetry(maxRetries, batchSize);
        for (OutboxEvent event : failedEvents) {
            claimAndPublish(event);
        }

        // Recover stale PROCESSING events (publisher crashed after claiming)
        LocalDateTime staleThreshold = LocalDateTime.now().minusSeconds(confirmTimeoutMs / 1000 * 3);
        List<OutboxEvent> staleEvents = outboxEventRepository.findStaleProcessingEvents(staleThreshold, batchSize);
        for (OutboxEvent event : staleEvents) {
            log.warn("[OUTBOX-RECOVERY] Recovering stale PROCESSING event: eventId={}, lastAttempt={}",
                    event.getEventId(), event.getLastAttemptAt());
            event.setStatus(OutboxEvent.OutboxStatus.FAILED);
            event.setLastError("Recovered from stale PROCESSING (publisher crash)");
            outboxEventRepository.save(event);
        }
    }

    // ─────────────────────────────────────────────
    //  SCHEDULED: Cleanup old SENT events
    // ─────────────────────────────────────────────

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void cleanupSentEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = outboxEventRepository.deleteSentEventsBefore(cutoff);
        if (deleted > 0) {
            log.info("[OUTBOX-CLEANUP] Deleted {} sent events older than 7 days", deleted);
        }
    }

    // ─────────────────────────────────────────────
    //  CORE: Claim → Publish → Confirm → update status
    // ─────────────────────────────────────────────

    private void claimAndPublish(OutboxEvent event) {
        // STEP 1: Transition PENDING/FAILED → PROCESSING
        event.setStatus(OutboxEvent.OutboxStatus.PROCESSING);
        event.setLastAttemptAt(LocalDateTime.now());
        outboxEventRepository.save(event);

        // STEP 2: Publish with correlationData for broker ACK matching
        CorrelationData correlationData = new CorrelationData(event.getEventId());

        try {
            rabbitTemplate.convertAndSend(
                    event.getExchange(),
                    event.getRoutingKey(),
                    event.getPayload(),
                    message -> {
                        message.getMessageProperties().setHeader("x-event-id", event.getEventId());
                        message.getMessageProperties().setHeader("x-event-type", event.getEventType());
                        message.getMessageProperties().setHeader("x-event-version", "1");
                        return message;
                    },
                    correlationData
            );

            // STEP 3: Wait for broker ACK (publisher confirm)
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(confirmTimeoutMs, TimeUnit.MILLISECONDS);

            if (confirm != null && confirm.isAck()) {
                // ✅ Broker confirmed receipt
                event.setStatus(OutboxEvent.OutboxStatus.SENT);
                event.setProcessedAt(LocalDateTime.now());
                event.setLastError(null);
                outboxEventRepository.save(event);

                publishSuccessCounter.increment();
                log.info("[OUTBOX] Published & confirmed: eventId={}, type={}, routingKey={}",
                        event.getEventId(), event.getEventType(), event.getRoutingKey());
            } else {
                // ❌ Broker NACK'd the message
                String reason = confirm != null ? confirm.getReason() : "NACK without reason";
                markFailed(event, "Broker NACK: " + reason);
            }

        } catch (Exception e) {
            // ❌ Timeout or connection failure
            markFailed(event, "Publish exception: " + e.getMessage());
        }
    }

    private void markFailed(OutboxEvent event, String reason) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setStatus(OutboxEvent.OutboxStatus.FAILED);
        event.setLastError(reason);
        outboxEventRepository.save(event);

        publishFailureCounter.increment();
        log.error("[OUTBOX] Publish failed: eventId={}, type={}, attempt={}/{}, reason={}",
                event.getEventId(), event.getEventType(), event.getRetryCount(), maxRetries, reason);
    }
}

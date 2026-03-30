package com.skillsync.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.payment.entity.OutboxEvent;
import com.skillsync.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for writing events to the Outbox table within the same transaction
 * as the business entity write. This guarantees atomicity — if the business
 * write fails, the outbox event is also rolled back.
 *
 * <p>This replaces direct {@code rabbitTemplate.convertAndSend()} calls.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Saves an event to the outbox table within the caller's transaction.
     * The OutboxPublisher scheduler will pick it up and publish to RabbitMQ.
     *
     * @param exchange   RabbitMQ exchange name
     * @param routingKey RabbitMQ routing key
     * @param eventType  human-readable event type for logging/debugging
     * @param payload    the event payload object (will be serialized to JSON)
     * @return the generated event ID (UUID)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String saveEvent(String exchange, String routingKey, String eventType, Object payload) {
        String eventId = UUID.randomUUID().toString();

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .routingKey(routingKey)
                    .exchange(exchange)
                    .payload(jsonPayload)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.debug("[OUTBOX] Event saved: eventId={}, type={}, routingKey={}",
                    eventId, eventType, routingKey);

            return eventId;

        } catch (JsonProcessingException e) {
            log.error("[OUTBOX] Failed to serialize event payload: type={}, error={}",
                    eventType, e.getMessage(), e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}

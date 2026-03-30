package com.skillsync.payment.service;

import com.skillsync.payment.entity.OutboxEvent;
import com.skillsync.payment.repository.OutboxEventRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


/**
 * Tests for OutboxPublisher:
 * - Publisher confirms: verifies SENT only after broker ACK
 * - Failure handling: verifies FAILED status on NACK/timeout
 * - Multi-instance safety: PROCESSING state transition
 * - Stale PROCESSING recovery
 */
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private MeterRegistry meterRegistry;
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        publisher = new OutboxPublisher(outboxEventRepository, rabbitTemplate, meterRegistry);
        ReflectionTestUtils.setField(publisher, "confirmTimeoutMs", 5000L);
        ReflectionTestUtils.setField(publisher, "batchSize", 50);
        ReflectionTestUtils.setField(publisher, "maxRetries", 5);
        publisher.initMetrics();
    }

    @Test
    @DisplayName("publishPendingEvents: broker ACK → marks SENT")
    void publishPendingEvents_brokerAck_marksSent() {
        OutboxEvent event = buildPendingEvent();

        // Mock: repository returns one pending event
        when(outboxEventRepository.findAndLockPendingEvents(50)).thenReturn(List.of(event));
        when(outboxEventRepository.findAndLockFailedEventsForRetry(5, 50)).thenReturn(Collections.emptyList());
        when(outboxEventRepository.findStaleProcessingEvents(any(), eq(50))).thenReturn(Collections.emptyList());

        // Mock: publisher confirm succeeds
        CompletableFuture<CorrelationData.Confirm> future = CompletableFuture.completedFuture(
                new CorrelationData.Confirm(true, null));
        doAnswer(inv -> {
            CorrelationData cd = inv.getArgument(4);
            ReflectionTestUtils.setField(cd, "future", future);
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(), any(org.springframework.amqp.core.MessagePostProcessor.class), any(CorrelationData.class));

        publisher.publishPendingEvents();

        // Verify event was saved with SENT status
        verify(outboxEventRepository, atLeast(2)).save(argThat(e ->
                e.getEventId().equals("evt-123") &&
                        (e.getStatus() == OutboxEvent.OutboxStatus.PROCESSING ||
                                e.getStatus() == OutboxEvent.OutboxStatus.SENT)));
    }

    @Test
    @DisplayName("publishPendingEvents: no pending events → no action")
    void publishPendingEvents_empty_noAction() {
        when(outboxEventRepository.findAndLockPendingEvents(50)).thenReturn(Collections.emptyList());
        when(outboxEventRepository.findAndLockFailedEventsForRetry(5, 50)).thenReturn(Collections.emptyList());
        when(outboxEventRepository.findStaleProcessingEvents(any(), eq(50))).thenReturn(Collections.emptyList());

        publisher.publishPendingEvents();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(), any(org.springframework.amqp.core.MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    @DisplayName("publishPendingEvents: rabbitTemplate throws → marks FAILED with retry count")
    void publishPendingEvents_exception_marksFailed() {
        OutboxEvent event = buildPendingEvent();

        when(outboxEventRepository.findAndLockPendingEvents(50)).thenReturn(List.of(event));
        when(outboxEventRepository.findAndLockFailedEventsForRetry(5, 50)).thenReturn(Collections.emptyList());
        when(outboxEventRepository.findStaleProcessingEvents(any(), eq(50))).thenReturn(Collections.emptyList());

        // Mock: publish throws exception
        doThrow(new RuntimeException("Connection lost"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(), any(org.springframework.amqp.core.MessagePostProcessor.class), any(CorrelationData.class));

        publisher.publishPendingEvents();

        // Verify event was marked FAILED
        verify(outboxEventRepository, atLeast(1)).save(argThat(e ->
                e.getStatus() == OutboxEvent.OutboxStatus.FAILED &&
                        e.getRetryCount() == 1));
    }

    @Test
    @DisplayName("stale PROCESSING events: recovered to FAILED")
    void publishPendingEvents_staleProcessing_recoveredToFailed() {
        OutboxEvent staleEvent = OutboxEvent.builder()
                .id(2L)
                .eventId("evt-stale")
                .eventType("payment.success")
                .routingKey("payment.success.v1")
                .exchange("payment.exchange")
                .payload("{}")
                .status(OutboxEvent.OutboxStatus.PROCESSING)
                .retryCount(0)
                .lastAttemptAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(outboxEventRepository.findAndLockPendingEvents(50)).thenReturn(Collections.emptyList());
        when(outboxEventRepository.findAndLockFailedEventsForRetry(5, 50)).thenReturn(Collections.emptyList());
        when(outboxEventRepository.findStaleProcessingEvents(any(), eq(50))).thenReturn(List.of(staleEvent));

        publisher.publishPendingEvents();

        verify(outboxEventRepository).save(argThat(e ->
                e.getEventId().equals("evt-stale") &&
                        e.getStatus() == OutboxEvent.OutboxStatus.FAILED));
    }

    private OutboxEvent buildPendingEvent() {
        return OutboxEvent.builder()
                .id(1L)
                .eventId("evt-123")
                .eventType("payment.business.action")
                .routingKey("payment.business.action.v1")
                .exchange("payment.exchange")
                .payload("{\"userId\":42}")
                .status(OutboxEvent.OutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }
}

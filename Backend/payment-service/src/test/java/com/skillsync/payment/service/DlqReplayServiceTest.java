package com.skillsync.payment.service;

import com.skillsync.payment.config.RabbitMQConfig;
import com.skillsync.payment.entity.FailedEvent;
import com.skillsync.payment.repository.FailedEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for DlqReplayService:
 * - Replay re-publishes with original eventId
 * - Replay skipped if already replayed
 * - Skip marks as SKIPPED
 */
@ExtendWith(MockitoExtension.class)
class DlqReplayServiceTest {

    @Mock
    private FailedEventRepository failedEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private DlqReplayService replayService;

    @BeforeEach
    void setUp() {
        replayService = new DlqReplayService(failedEventRepository, rabbitTemplate, new SimpleMeterRegistry());
        replayService.initMetrics();
    }

    @Test
    @DisplayName("replayEvent: PENDING_REVIEW → re-publishes and marks REPLAYED")
    void replayEvent_pendingReview_replaysAndMarks() {
        FailedEvent event = FailedEvent.builder()
                .eventId("evt-fail-1")
                .sourceQueue("payment.business.action.dlq")
                .routingKey("payment.business.action.v1")
                .payload("{\"userId\":42}")
                .replayStatus(FailedEvent.ReplayStatus.PENDING_REVIEW)
                .failedAt(LocalDateTime.now())
                .build();

        when(failedEventRepository.findByEventId("evt-fail-1")).thenReturn(Optional.of(event));

        Map<String, String> result = replayService.replayEvent("evt-fail-1");

        assertEquals("REPLAYED", result.get("status"));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.PAYMENT_EXCHANGE),
                eq("payment.business.action.v1"), eq("{\"userId\":42}"), any(org.springframework.amqp.core.MessagePostProcessor.class));
        verify(failedEventRepository).save(argThat(e ->
                e.getReplayStatus() == FailedEvent.ReplayStatus.REPLAYED));
    }

    @Test
    @DisplayName("replayEvent: already REPLAYED → skipped")
    void replayEvent_alreadyReplayed_skipped() {
        FailedEvent event = FailedEvent.builder()
                .eventId("evt-fail-2")
                .replayStatus(FailedEvent.ReplayStatus.REPLAYED)
                .build();

        when(failedEventRepository.findByEventId("evt-fail-2")).thenReturn(Optional.of(event));

        Map<String, String> result = replayService.replayEvent("evt-fail-2");

        assertEquals("SKIPPED", result.get("status"));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(), any(org.springframework.amqp.core.MessagePostProcessor.class));
    }

    @Test
    @DisplayName("skipEvent: marks as SKIPPED")
    void skipEvent_marksSkipped() {
        FailedEvent event = FailedEvent.builder()
                .eventId("evt-fail-3")
                .replayStatus(FailedEvent.ReplayStatus.PENDING_REVIEW)
                .build();

        when(failedEventRepository.findByEventId("evt-fail-3")).thenReturn(Optional.of(event));

        Map<String, String> result = replayService.skipEvent("evt-fail-3");

        assertEquals("SKIPPED", result.get("status"));
        verify(failedEventRepository).save(argThat(e ->
                e.getReplayStatus() == FailedEvent.ReplayStatus.SKIPPED));
    }

    @Test
    @DisplayName("replayEvent: not found → throws")
    void replayEvent_notFound_throws() {
        when(failedEventRepository.findByEventId("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> replayService.replayEvent("missing"));
    }
}

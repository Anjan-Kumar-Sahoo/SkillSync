package com.skillsync.payment.service;

import com.skillsync.payment.entity.OutboxEvent;
import com.skillsync.payment.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherCoverageTest {

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private MeterRegistry meterRegistry;
    @Mock private Counter counter;

    @InjectMocks private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() throws Exception {
        // Use reflection to set private counter fields
        var successField = OutboxPublisher.class.getDeclaredField("publishSuccessCounter");
        successField.setAccessible(true);
        successField.set(outboxPublisher, counter);

        var failureField = OutboxPublisher.class.getDeclaredField("publishFailureCounter");
        failureField.setAccessible(true);
        failureField.set(outboxPublisher, counter);

        // Set timeout ms for get() call
        var timeoutField = OutboxPublisher.class.getDeclaredField("confirmTimeoutMs");
        timeoutField.setAccessible(true);
        timeoutField.set(outboxPublisher, 1000L);
    }

    @Test @DisplayName("publishPendingEvents - Recover stale PROCESSING events")
    void publishPendingEventsStaleRecovery() {
        OutboxEvent staleEvent = new OutboxEvent();
        staleEvent.setEventId("stale-1");
        staleEvent.setStatus(OutboxEvent.OutboxStatus.PROCESSING);
        
        when(outboxEventRepository.findAndLockPendingEvents(anyInt())).thenReturn(Collections.emptyList());
        when(outboxEventRepository.findAndLockFailedEventsForRetry(anyInt(), anyInt())).thenReturn(Collections.emptyList());
        when(outboxEventRepository.findStaleProcessingEvents(any(), anyInt())).thenReturn(List.of(staleEvent));

        outboxPublisher.publishPendingEvents();

        verify(outboxEventRepository).save(argThat(e -> 
            e.getStatus() == OutboxEvent.OutboxStatus.FAILED && e.getLastError().contains("stale PROCESSING")
        ));
    }

    @Test @DisplayName("cleanupSentEvents - with deletions")
    void cleanupSentEventsWithDeletions() {
        when(outboxEventRepository.deleteSentEventsBefore(any())).thenReturn(5);
        outboxPublisher.cleanupSentEvents();
        verify(outboxEventRepository).deleteSentEventsBefore(any());
    }

    @Test @DisplayName("claimAndPublish - Broker NACK")
    void claimAndPublishNack() throws Exception {
        OutboxEvent event = createEvent("evt-nack");
        when(outboxEventRepository.findAndLockPendingEvents(anyInt())).thenReturn(List.of(event));
        
        CorrelationData correlationData = new CorrelationData("evt-nack");
        // Mock a NACK confirm
        CorrelationData.Confirm nackConfirm = new CorrelationData.Confirm(false, "Broker full");
        correlationData.getFuture().complete(nackConfirm);
        
        // We need to capture the CorrelationData passed to rabbitTemplate to return our future
        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(4);
            cd.getFuture().complete(nackConfirm);
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(), any(), any(CorrelationData.class));

        outboxPublisher.publishPendingEvents();

        verify(outboxEventRepository, atLeastOnce()).save(argThat(e -> 
            e.getStatus() == OutboxEvent.OutboxStatus.FAILED && e.getLastError().contains("Broker NACK")
        ));
    }

    @Test @DisplayName("claimAndPublish - Timeout exception")
    void claimAndPublishTimeout() throws Exception {
        OutboxEvent event = createEvent("evt-timeout");
        when(outboxEventRepository.findAndLockPendingEvents(anyInt())).thenReturn(List.of(event));
        
        // Mock a timeout (future that never completes or throws)
        doAnswer(invocation -> {
            throw new RuntimeException("Timeout waiting for confirm");
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(), any(), any(CorrelationData.class));

        outboxPublisher.publishPendingEvents();

        verify(outboxEventRepository, atLeastOnce()).save(argThat(e -> 
            e.getStatus() == OutboxEvent.OutboxStatus.FAILED && e.getLastError().contains("Publish exception")
        ));
    }

    @Test @DisplayName("claimAndPublish - confirm is null")
    void claimAndPublishNullConfirm() throws Exception {
        OutboxEvent event = createEvent("evt-null");
        when(outboxEventRepository.findAndLockPendingEvents(anyInt())).thenReturn(List.of(event));
        
        doAnswer(invocation -> {
            CorrelationData cd = invocation.getArgument(4);
            cd.getFuture().complete(null); // Return null confirm
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(), any(), any(CorrelationData.class));

        outboxPublisher.publishPendingEvents();

        verify(outboxEventRepository, atLeastOnce()).save(argThat(e -> 
            e.getStatus() == OutboxEvent.OutboxStatus.FAILED && e.getLastError().contains("NACK without reason")
        ));
    }

    private OutboxEvent createEvent(String id) {
        OutboxEvent event = new OutboxEvent();
        event.setEventId(id);
        event.setStatus(OutboxEvent.OutboxStatus.PENDING);
        event.setExchange("test-ex");
        event.setRoutingKey("test-rk");
        event.setPayload("{}");
        event.setRetryCount(0);
        return event;
    }
}

package com.skillsync.user.consumer;

import com.skillsync.user.entity.ProcessedEvent;
import com.skillsync.user.repository.ProcessedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock private ProcessedEventRepository processedEventRepository;

    @InjectMocks private PaymentEventConsumer paymentEventConsumer;

    private Map<String, Object> buildEvent(String eventId, String paymentType, Long userId, Long referenceId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", eventId);
        event.put("paymentType", paymentType);
        event.put("userId", userId);
        event.put("referenceId", referenceId);
        event.put("orderId", "order_123");
        return event;
    }

    @Test
    @DisplayName("processes SESSION_BOOKING event and records it")
    void shouldProcessSessionBooking() {
        Map<String, Object> event = buildEvent("evt-1", "SESSION_BOOKING", 100L, 200L);
        when(processedEventRepository.existsByEventId("evt-1")).thenReturn(false);

        paymentEventConsumer.handlePaymentBusinessAction(event);

        verify(processedEventRepository).save(argThat(pe -> "evt-1".equals(pe.getEventId())));
    }

    @Test
    @DisplayName("skips duplicate event (idempotency)")
    void shouldSkipDuplicateEvent() {
        Map<String, Object> event = buildEvent("evt-1", "SESSION_BOOKING", 100L, 200L);
        when(processedEventRepository.existsByEventId("evt-1")).thenReturn(true);

        paymentEventConsumer.handlePaymentBusinessAction(event);

        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("handles unknown payment type without error")
    void shouldHandleUnknownPaymentType() {
        Map<String, Object> event = buildEvent("evt-2", "UNKNOWN_TYPE", 100L, 200L);
        when(processedEventRepository.existsByEventId("evt-2")).thenReturn(false);

        paymentEventConsumer.handlePaymentBusinessAction(event);

        // Should still record as processed
        verify(processedEventRepository).save(argThat(pe -> "evt-2".equals(pe.getEventId())));
    }

    @Test
    @DisplayName("handles null eventId gracefully")
    void shouldHandleNullEventId() {
        Map<String, Object> event = buildEvent(null, "SESSION_BOOKING", 100L, 200L);

        paymentEventConsumer.handlePaymentBusinessAction(event);

        // Should not attempt to save processed event when eventId is null
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("handles numeric userId as Integer (JSON deserialization)")
    void shouldHandleIntegerUserId() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", "evt-3");
        event.put("paymentType", "SESSION_BOOKING");
        event.put("userId", 100); // Integer, not Long
        event.put("referenceId", 200);
        event.put("orderId", "order_456");
        when(processedEventRepository.existsByEventId("evt-3")).thenReturn(false);

        paymentEventConsumer.handlePaymentBusinessAction(event);

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("handles null userId and referenceId")
    void shouldHandleNullValues() {
        Map<String, Object> event = buildEvent("evt-4", "SESSION_BOOKING", null, null);
        when(processedEventRepository.existsByEventId("evt-4")).thenReturn(false);

        paymentEventConsumer.handlePaymentBusinessAction(event);

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }
}

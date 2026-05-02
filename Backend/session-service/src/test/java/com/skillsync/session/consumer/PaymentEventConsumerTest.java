package com.skillsync.session.consumer;

import com.skillsync.session.event.PaymentCompletedEvent;
import com.skillsync.session.service.command.SessionCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private SessionCommandService sessionCommandService;

    @InjectMocks
    private PaymentEventConsumer consumer;

    private PaymentCompletedEvent createEvent(String type, Long refId) {
        return new PaymentCompletedEvent(
                "evt-1",
                "1.0",
                "2026-05-02T12:00:00",
                1L,
                "order-1",
                type,
                "SUCCESS",
                1000,
                refId,
                "SESSION",
                "reason"
        );
    }

    @Test
    void handlePaymentSuccess_validEvent() {
        PaymentCompletedEvent event = createEvent("SESSION_BOOKING", 100L);
        consumer.handlePaymentSuccess(event);
        verify(sessionCommandService).confirmSessionPayment(100L);
    }

    @Test
    void handlePaymentSuccess_invalidType() {
        PaymentCompletedEvent event = createEvent("OTHER_TYPE", 100L);
        consumer.handlePaymentSuccess(event);
        verify(sessionCommandService, never()).confirmSessionPayment(any());
    }

    @Test
    void handlePaymentSuccess_nullReference() {
        PaymentCompletedEvent event = createEvent("SESSION_BOOKING", null);
        consumer.handlePaymentSuccess(event);
        verify(sessionCommandService, never()).confirmSessionPayment(any());
    }

    @Test
    void handlePaymentSuccess_exception() {
        PaymentCompletedEvent event = createEvent("SESSION_BOOKING", 100L);
        doThrow(new RuntimeException("DB Error")).when(sessionCommandService).confirmSessionPayment(100L);
        assertThrows(RuntimeException.class, () -> consumer.handlePaymentSuccess(event));
    }

    @Test
    void handlePaymentFailed_validEvent() {
        PaymentCompletedEvent event = createEvent("SESSION_BOOKING", 100L);
        consumer.handlePaymentFailed(event);
        verify(sessionCommandService).rollbackSessionPayment(100L, 1L, "reason");
    }

    @Test
    void handlePaymentFailed_invalidType() {
        PaymentCompletedEvent event = createEvent("OTHER_TYPE", 100L);
        consumer.handlePaymentFailed(event);
        verify(sessionCommandService, never()).rollbackSessionPayment(any(), any(), any());
    }

    @Test
    void handlePaymentFailed_exception() {
        PaymentCompletedEvent event = createEvent("SESSION_BOOKING", 100L);
        doThrow(new RuntimeException("DB Error")).when(sessionCommandService).rollbackSessionPayment(100L, 1L, "reason");
        assertThrows(RuntimeException.class, () -> consumer.handlePaymentFailed(event));
    }

    @Test
    void handlePaymentCompensated_validEvent() {
        PaymentCompletedEvent event = createEvent("SESSION_BOOKING", 100L);
        consumer.handlePaymentCompensated(event);
        verify(sessionCommandService).rollbackSessionPayment(100L, 1L, "reason");
    }

    @Test
    void handlePaymentCompensated_invalidType() {
        PaymentCompletedEvent event = createEvent("OTHER_TYPE", 100L);
        consumer.handlePaymentCompensated(event);
        verify(sessionCommandService, never()).rollbackSessionPayment(any(), any(), any());
    }

    @Test
    void handlePaymentCompensated_exception() {
        PaymentCompletedEvent event = createEvent("SESSION_BOOKING", 100L);
        doThrow(new RuntimeException("DB Error")).when(sessionCommandService).rollbackSessionPayment(100L, 1L, "reason");
        assertThrows(RuntimeException.class, () -> consumer.handlePaymentCompensated(event));
    }
}

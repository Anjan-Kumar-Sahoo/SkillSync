package com.skillsync.session.consumer;

import com.skillsync.session.event.PaymentCompletedEvent;
import com.skillsync.session.service.command.SessionCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerCoverageTest {

    @Mock private SessionCommandService sessionCommandService;
    @InjectMocks private PaymentEventConsumer paymentEventConsumer;

    @Test @DisplayName("handlePaymentSuccess - various branches")
    void handlePaymentSuccessBranches() {
        // Case 1: SESSION_BOOKING (Success)
        PaymentCompletedEvent successEvent = new PaymentCompletedEvent(
                "evt-1", "1", "now", 1L, "ord-1", "SESSION_BOOKING", "SUCCESS", 100, 100L, "SESSION", null
        );
        paymentEventConsumer.handlePaymentSuccess(successEvent);
        verify(sessionCommandService).confirmSessionPayment(100L);

        // Case 2: Other type (Ignored)
        PaymentCompletedEvent otherEvent = new PaymentCompletedEvent(
                "evt-2", "1", "now", 1L, "ord-2", "OTHER", "SUCCESS", 100, 100L, "SESSION", null
        );
        paymentEventConsumer.handlePaymentSuccess(otherEvent);
        verify(sessionCommandService, times(1)).confirmSessionPayment(100L);
        
        // Case 3: Exception catch
        doThrow(new RuntimeException("DB error")).when(sessionCommandService).confirmSessionPayment(anyLong());
        assertThrows(RuntimeException.class, () -> paymentEventConsumer.handlePaymentSuccess(successEvent));
    }

    @Test @DisplayName("handlePaymentFailed - branches")
    void handlePaymentFailedBranches() {
        // Case 1: SESSION_BOOKING (Failure)
        PaymentCompletedEvent failEvent = new PaymentCompletedEvent(
                "evt-3", "1", "now", 1L, "ord-3", "SESSION_BOOKING", "FAILED", 100, 100L, "SESSION", "Insufficient funds"
        );
        paymentEventConsumer.handlePaymentFailed(failEvent);
        verify(sessionCommandService).rollbackSessionPayment(100L, 1L, "Insufficient funds");

        // Case 2: Null referenceId
        PaymentCompletedEvent nullRefEvent = new PaymentCompletedEvent(
                "evt-4", "1", "now", 1L, "ord-4", "SESSION_BOOKING", "FAILED", 100, null, "SESSION", "Error"
        );
        paymentEventConsumer.handlePaymentFailed(nullRefEvent);
        verify(sessionCommandService, times(1)).rollbackSessionPayment(anyLong(), any(), any());
    }

    @Test @DisplayName("handlePaymentCompensated - coverage")
    void handlePaymentCompensated() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                "evt-5", "1", "now", 1L, "ord-5", "SESSION_BOOKING", "COMPENSATED", 100, 100L, "SESSION", "Refunded"
        );
        paymentEventConsumer.handlePaymentCompensated(event);
        verify(sessionCommandService).rollbackSessionPayment(eq(100L), any(), any());
    }
}

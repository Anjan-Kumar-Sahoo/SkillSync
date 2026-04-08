package com.skillsync.session.consumer;

import com.skillsync.session.config.RabbitMQConfig;
import com.skillsync.session.event.PaymentCompletedEvent;
import com.skillsync.session.service.command.SessionCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final SessionCommandService sessionCommandService;

    @RabbitListener(queues = RabbitMQConfig.SESSION_PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(PaymentCompletedEvent event) {
        log.info("[SESSION-CONSUMER] Received payment success event: orderId={}, referenceId={}, type={}",
                event.orderId(), event.referenceId(), event.type());

        try {
            if ("SESSION_BOOKING".equals(event.type()) && event.referenceId() != null) {
                // Confirm the session because payment is successful
                sessionCommandService.confirmSessionPayment(event.referenceId());
                log.info("[SESSION-CONSUMER] Successfully confirmed session {} after payment.", event.referenceId());
            } else {
                log.warn("[SESSION-CONSUMER] Ignored event (type mismatch or null referenceId): {}", event);
            }
        } catch (Exception e) {
            log.error("[SESSION-CONSUMER] Error processing payment success for sessionId={}: {}", event.referenceId(), e.getMessage(), e);
            // Ideally should send to a DLQ or trigger a compensation, but logging for now
            throw e; // rethrow is important for DLQ if configured
        }
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_PAYMENT_FAILED_QUEUE)
    public void handlePaymentFailed(PaymentCompletedEvent event) {
        log.info("[SESSION-CONSUMER] Received payment failed event: orderId={}, referenceId={}, type={}, reason={}",
                event.orderId(), event.referenceId(), event.type(), event.compensationReason());

        try {
            if ("SESSION_BOOKING".equals(event.type()) && event.referenceId() != null) {
                sessionCommandService.rollbackSessionPayment(
                        event.referenceId(),
                        event.userId(),
                        event.compensationReason()
                );
                log.info("[SESSION-CONSUMER] Rolled back provisional session {} after payment failure.", event.referenceId());
            }
        } catch (Exception e) {
            log.error("[SESSION-CONSUMER] Error rolling back failed payment for sessionId={}: {}",
                    event.referenceId(), e.getMessage(), e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_PAYMENT_COMPENSATED_QUEUE)
    public void handlePaymentCompensated(PaymentCompletedEvent event) {
        log.info("[SESSION-CONSUMER] Received payment compensated event: orderId={}, referenceId={}, type={}, reason={}",
                event.orderId(), event.referenceId(), event.type(), event.compensationReason());

        try {
            if ("SESSION_BOOKING".equals(event.type()) && event.referenceId() != null) {
                sessionCommandService.rollbackSessionPayment(
                        event.referenceId(),
                        event.userId(),
                        event.compensationReason()
                );
                log.info("[SESSION-CONSUMER] Rolled back session {} after payment compensation.", event.referenceId());
            }
        } catch (Exception e) {
            log.error("[SESSION-CONSUMER] Error rolling back compensated payment for sessionId={}: {}",
                    event.referenceId(), e.getMessage(), e);
            throw e;
        }
    }
}

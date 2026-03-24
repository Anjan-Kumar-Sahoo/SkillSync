package com.skillsync.notification.consumer;

import com.skillsync.notification.config.RabbitMQConfig;
import com.skillsync.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes payment lifecycle events from the User Service (PaymentSagaOrchestrator).
 * Creates and pushes notifications for payment SUCCESS, FAILED, and COMPENSATED states.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_NOTIFICATION_SUCCESS_QUEUE)
    public void handlePaymentSuccess(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        String paymentType = (String) event.get("paymentType");
        String orderId = (String) event.get("orderId");

        String title = "Payment Successful! ✅";
        String message = buildSuccessMessage(paymentType, orderId);

        notificationService.createAndPush(userId, "PAYMENT_SUCCESS", title, message);
        log.info("Processed PAYMENT_SUCCESS event for user {}, orderId={}", userId, orderId);
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_NOTIFICATION_FAILED_QUEUE)
    public void handlePaymentFailed(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        String paymentType = (String) event.get("paymentType");
        String orderId = (String) event.get("orderId");
        String reason = (String) event.get("compensationReason");

        String title = "Payment Failed ❌";
        String message = buildFailedMessage(paymentType, orderId, reason);

        notificationService.createAndPush(userId, "PAYMENT_FAILED", title, message);
        log.info("Processed PAYMENT_FAILED event for user {}, orderId={}", userId, orderId);
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_NOTIFICATION_COMPENSATED_QUEUE)
    public void handlePaymentCompensated(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        String paymentType = (String) event.get("paymentType");
        String orderId = (String) event.get("orderId");
        String reason = (String) event.get("compensationReason");

        String title = "Payment Issue — Action Required ⚠️";
        String message = buildCompensatedMessage(paymentType, orderId, reason);

        notificationService.createAndPush(userId, "PAYMENT_COMPENSATED", title, message);
        log.info("Processed PAYMENT_COMPENSATED event for user {}, orderId={}", userId, orderId);
    }

    // ─── Message Builders ───

    private String buildSuccessMessage(String paymentType, String orderId) {
        return switch (paymentType) {
            case "MENTOR_FEE" -> "Your mentor onboarding fee has been processed successfully. " +
                    "Your mentor profile is now active! (Order: " + orderId + ")";
            case "SESSION_BOOKING" -> "Your session booking payment was successful. " +
                    "You can now proceed with your session. (Order: " + orderId + ")";
            default -> "Your payment of ₹9 was successful. (Order: " + orderId + ")";
        };
    }

    private String buildFailedMessage(String paymentType, String orderId, String reason) {
        String base = switch (paymentType) {
            case "MENTOR_FEE" -> "Your mentor onboarding fee payment could not be verified.";
            case "SESSION_BOOKING" -> "Your session booking payment could not be verified.";
            default -> "Your payment could not be verified.";
        };
        return base + " Please try again with a new payment. (Order: " + orderId + ")";
    }

    private String buildCompensatedMessage(String paymentType, String orderId, String reason) {
        String base = switch (paymentType) {
            case "MENTOR_FEE" -> "Your mentor onboarding payment was received, but we encountered " +
                    "an issue processing your mentor activation.";
            case "SESSION_BOOKING" -> "Your session booking payment was received, but we encountered " +
                    "an issue completing your booking.";
            default -> "Your payment was received, but a processing issue occurred.";
        };
        return base + " Our team has been notified and will resolve this shortly. " +
                "Your payment is on record. (Order: " + orderId + ")";
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }
}

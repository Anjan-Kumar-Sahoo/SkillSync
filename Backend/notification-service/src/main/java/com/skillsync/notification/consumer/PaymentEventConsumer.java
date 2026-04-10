package com.skillsync.notification.consumer;

import com.skillsync.notification.config.RabbitMQConfig;
import com.skillsync.notification.dto.UserSummary;
import com.skillsync.notification.feign.AuthServiceClient;
import com.skillsync.notification.service.EmailService;
import com.skillsync.notification.service.command.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consumes payment lifecycle events and pushes notifications + styled emails.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final NotificationCommandService notificationCommandService;
    private final EmailService emailService;
    private final AuthServiceClient authServiceClient;

    @Value("${app.base-url:https://skillsync.mraks.dev}")
    private String appBaseUrl;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_NOTIFICATION_SUCCESS_QUEUE)
    public void handlePaymentSuccess(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        String paymentType = String.valueOf(event.get("paymentType"));
        String orderId = String.valueOf(event.get("orderId"));
        String amount = formatAmount(event.get("amount"));

        String title = "Payment Successful! ✅";
        String message = buildSuccessMessage(paymentType, orderId);

        notificationCommandService.createAndPush(userId, "PAYMENT_SUCCESS", title, message);

        try {
            UserSummary user = authServiceClient.getUserById(userId);
            Map<String, String> details = new LinkedHashMap<>();
            details.put("Payment For", paymentType.replace("_", " "));
            details.put("Order ID", orderId);
            details.put("Amount", amount);
            details.put("Status", "Successful");

            emailService.sendEmail(
                    user.email(),
                    "SkillSync Payment Confirmation",
                    "system-email",
                    Map.of(
                            "recipientName", displayName(user),
                            "title", "Payment Confirmed",
                            "preheader", "Your payment was processed successfully.",
                            "primaryMessage", "Your transaction is complete and the booking workflow has been updated.",
                            "detailsTitle", "Important details",
                            "detailsHtml", emailService.buildDetailsHtml(details),
                            "actionText", "View Sessions",
                            "actionUrl", appBaseUrl + "/sessions",
                            "footerNote", "If this was unexpected, contact support immediately."
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send payment success email to user {}: {}", userId, e.getMessage());
        }

        log.info("Processed PAYMENT_SUCCESS event for user {}, orderId={}", userId, orderId);
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_NOTIFICATION_FAILED_QUEUE)
    public void handlePaymentFailed(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        String paymentType = String.valueOf(event.get("paymentType"));
        String orderId = String.valueOf(event.get("orderId"));
        String reason = normalizeReason(event.get("compensationReason"));
        String amount = formatAmount(event.get("amount"));

        String title = "Payment Failed ❌";
        String message = buildFailedMessage(paymentType, orderId, reason);

        notificationCommandService.createAndPush(userId, "PAYMENT_FAILED", title, message);

        try {
            UserSummary user = authServiceClient.getUserById(userId);
            Map<String, String> details = new LinkedHashMap<>();
            details.put("Payment For", paymentType.replace("_", " "));
            details.put("Order ID", orderId);
            details.put("Amount", amount);
            details.put("Reason", reason);

            emailService.sendEmail(
                    user.email(),
                    "Payment Failed - SkillSync",
                    "system-email",
                    Map.of(
                            "recipientName", displayName(user),
                            "title", "Payment Failed",
                            "preheader", "Your transaction could not be completed.",
                            "primaryMessage", "No worries. You can retry the payment from your bookings page.",
                            "detailsTitle", "Important details",
                            "detailsHtml", emailService.buildDetailsHtml(details),
                            "actionText", "Retry Payment",
                            "actionUrl", appBaseUrl + "/sessions",
                            "footerNote", "Use a fresh payment attempt to avoid duplicate authorization holds."
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send payment failure email to user {}: {}", userId, e.getMessage());
        }

        log.info("Processed PAYMENT_FAILED event for user {}, orderId={}", userId, orderId);
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_NOTIFICATION_COMPENSATED_QUEUE)
    public void handlePaymentCompensated(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        String paymentType = String.valueOf(event.get("paymentType"));
        String orderId = String.valueOf(event.get("orderId"));
        String reason = normalizeReason(event.get("compensationReason"));
        String amount = formatAmount(event.get("amount"));

        String title = "Payment Issue — Action Required ⚠️";
        String message = buildCompensatedMessage(paymentType, orderId, reason);

        notificationCommandService.createAndPush(userId, "PAYMENT_COMPENSATED", title, message);

        try {
            UserSummary user = authServiceClient.getUserById(userId);
            Map<String, String> details = new LinkedHashMap<>();
            details.put("Payment For", paymentType.replace("_", " "));
            details.put("Order ID", orderId);
            details.put("Amount", amount);
            details.put("Issue", reason);

            emailService.sendEmail(
                    user.email(),
                    "Payment Issue Update - SkillSync",
                    "system-email",
                    Map.of(
                            "recipientName", displayName(user),
                            "title", "Payment Issue Update",
                            "preheader", "Your payment is received but booking completion needs attention.",
                            "primaryMessage", "Our system flagged a processing issue and initiated recovery safeguards.",
                            "detailsTitle", "Important details",
                            "detailsHtml", emailService.buildDetailsHtml(details),
                            "actionText", "Open Support",
                            "actionUrl", appBaseUrl + "/help",
                            "footerNote", "Our team has been notified. Keep this email for reference."
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send payment compensated email to user {}: {}", userId, e.getMessage());
        }

        log.info("Processed PAYMENT_COMPENSATED event for user {}, orderId={}", userId, orderId);
    }

    private String buildSuccessMessage(String paymentType, String orderId) {
        return switch (paymentType) {
            case "SESSION_BOOKING" -> "Your session booking payment was successful. You can now proceed with your session. (Order: " + orderId + ")";
            default -> "Your payment was successful. (Order: " + orderId + ")";
        };
    }

    private String buildFailedMessage(String paymentType, String orderId, String reason) {
        String base = switch (paymentType) {
            case "SESSION_BOOKING" -> "Your session booking payment could not be verified.";
            default -> "Your payment could not be verified.";
        };
        return base + " Please try again with a new payment. (Order: " + orderId + ")";
    }

    private String buildCompensatedMessage(String paymentType, String orderId, String reason) {
        String base = switch (paymentType) {
            case "SESSION_BOOKING" -> "Your session booking payment was received, but we encountered an issue completing your booking.";
            default -> "Your payment was received, but a processing issue occurred.";
        };
        return base + " Our team has been notified and will resolve this shortly. (Order: " + orderId + ")";
    }

    private String formatAmount(Object amountRaw) {
        if (!(amountRaw instanceof Number number)) {
            return "₹0.00";
        }
        double amountRupees = number.doubleValue() / 100.0;
        return String.format("₹%.2f", amountRupees);
    }

    private String normalizeReason(Object reasonRaw) {
        if (reasonRaw == null) {
            return "Not specified";
        }
        String reason = String.valueOf(reasonRaw).trim();
        if (reason.isEmpty() || "null".equalsIgnoreCase(reason)) {
            return "Not specified";
        }
        return reason;
    }

    private String displayName(UserSummary user) {
        String first = user.firstName() != null ? user.firstName().trim() : "";
        String last = user.lastName() != null ? user.lastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.email() : full;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}

package com.skillsync.notification.consumer;

import com.skillsync.notification.config.RabbitMQConfig;
import com.skillsync.notification.dto.EmailRetryEvent;
import com.skillsync.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumes failed email retry events from email.retry.queue.
 * Implements exponential backoff with max 3 retries.
 *
 * Retry policy:
 *   Attempt 1: immediate (from initial failure)
 *   Attempt 2: 2s delay
 *   Attempt 3: 4s delay
 *   After 3 failures: logged permanently, message goes to DLQ
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailRetryConsumer {

    private final EmailService emailService;
    private final RabbitTemplate rabbitTemplate;

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 2000; // 2 seconds

    @RabbitListener(queues = RabbitMQConfig.EMAIL_RETRY_QUEUE)
    public void handleEmailRetry(EmailRetryEvent event) {
        int currentRetry = event.retryCount() + 1;
        log.info("[EMAIL_RETRY] Processing retry {}/{} | to={} | emailType={}",
                currentRetry, MAX_RETRIES, event.to(), event.templateName());

        // Exponential backoff delay
        if (currentRetry > 1) {
            long delay = BASE_DELAY_MS * (long) Math.pow(2, currentRetry - 2);
            try {
                log.debug("[EMAIL_RETRY] Waiting {}ms before retry {}", delay, currentRetry);
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[EMAIL_RETRY] Interrupted during backoff");
            }
        }

        try {
            emailService.doSendEmail(event.to(), event.subject(), event.templateName(), event.variables());
            log.info("[EMAIL_RETRY] Successfully sent on retry {}/{} | to={} | emailType={}",
                    currentRetry, MAX_RETRIES, event.to(), event.templateName());
        } catch (Exception e) {
            log.error("[EMAIL_RETRY] Attempt {}/{} failed | to={} | emailType={} | error={}",
                    currentRetry, MAX_RETRIES, event.to(), event.templateName(), e.getMessage());

            if (currentRetry < MAX_RETRIES) {
                // Re-publish with incremented retry count
                EmailRetryEvent retryEvent = new EmailRetryEvent(
                        event.to(), event.subject(), event.templateName(),
                        event.variables(), currentRetry, e.getMessage());
                rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_RETRY_EXCHANGE, "email.retry", retryEvent);
                log.warn("[EMAIL_RETRY] Re-queued for retry {}/{} | to={}", currentRetry + 1, MAX_RETRIES, event.to());
            } else {
                log.error("[EMAIL_RETRY] PERMANENT FAILURE after {} retries | to={} | emailType={} | lastError={}",
                        MAX_RETRIES, event.to(), event.templateName(), e.getMessage());
                // Message will be DLQ'd by RabbitMQ if configured, or simply logged permanently
            }
        }
    }
}

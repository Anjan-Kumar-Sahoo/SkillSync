package com.skillsync.notification.service;

import com.skillsync.notification.config.RabbitMQConfig;
import com.skillsync.notification.dto.EmailRetryEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final RabbitTemplate rabbitTemplate;

    private static final int MAX_RETRIES = 3;

    /**
     * Sends an email using a Thymeleaf template.
     * On failure, publishes to email.retry.queue for async retry.
     *
     * @param to           recipient email
     * @param subject      email subject
     * @param templateName template file name (without .html)
     * @param variables    variables for the template
     */
    @Async
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        log.info("[EMAIL] Sending {} email | to={} | emailType={}", templateName, to, templateName);
        try {
            doSendEmail(to, subject, templateName, variables);
            log.info("[EMAIL] Successfully sent | to={} | emailType={}", to, templateName);
        } catch (MessagingException e) {
            log.error("[EMAIL] Failed to send | to={} | emailType={} | error={}", to, templateName, e.getMessage());
            publishRetryEvent(to, subject, templateName, variables, 0, e.getMessage());
        } catch (Exception e) {
            log.error("[EMAIL] Unexpected error | to={} | emailType={} | error={}", to, templateName, e.getMessage());
            publishRetryEvent(to, subject, templateName, variables, 0, e.getMessage());
        }
    }

    /**
     * Core email sending logic — used by both initial send and retry consumer.
     */
    public void doSendEmail(String to, String subject, String templateName, Map<String, Object> variables)
            throws MessagingException {
        Context context = new Context();
        context.setVariables(variables);
        String htmlContent = templateEngine.process("emails/" + templateName, context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Publishes a retry event to the email retry queue.
     */
    private void publishRetryEvent(String to, String subject, String templateName,
                                    Map<String, Object> variables, int retryCount, String reason) {
        if (retryCount >= MAX_RETRIES) {
            log.error("[EMAIL] PERMANENT FAILURE after {} retries | to={} | emailType={} | lastError={}",
                    retryCount, to, templateName, reason);
            return;
        }
        EmailRetryEvent event = new EmailRetryEvent(to, subject, templateName, variables, retryCount, reason);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_RETRY_EXCHANGE, "email.retry", event);
        log.warn("[EMAIL] Retry event published | to={} | emailType={} | retryCount={}", to, templateName, retryCount);
    }

    public int getMaxRetries() {
        return MAX_RETRIES;
    }
}

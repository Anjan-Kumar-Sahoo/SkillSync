package com.skillsync.notification.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Event published to the email retry queue when an email fails to send.
 * Consumed by EmailRetryConsumer with exponential backoff.
 */
public record EmailRetryEvent(
    String to,
    String subject,
    String templateName,
    Map<String, Object> variables,
    int retryCount,
    String failureReason
) implements Serializable {}

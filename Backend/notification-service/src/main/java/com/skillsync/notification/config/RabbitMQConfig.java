package com.skillsync.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Session events
    public static final String SESSION_EXCHANGE = "session.exchange";
    public static final String SESSION_NOTIFICATION_REQUESTED_QUEUE = "notification.session.requested.queue";
    public static final String SESSION_NOTIFICATION_ACCEPTED_QUEUE = "notification.session.accepted.queue";
    public static final String SESSION_NOTIFICATION_REJECTED_QUEUE = "notification.session.rejected.queue";
    public static final String SESSION_NOTIFICATION_CANCELLED_QUEUE = "notification.session.cancelled.queue";
    public static final String SESSION_NOTIFICATION_COMPLETED_QUEUE = "notification.session.completed.queue";

    // Mentor events
    public static final String MENTOR_EXCHANGE = "mentor.exchange";
    public static final String MENTOR_NOTIFICATION_APPROVED_QUEUE = "notification.mentor.approved.queue";
    public static final String MENTOR_NOTIFICATION_REJECTED_QUEUE = "notification.mentor.rejected.queue";
    public static final String MENTOR_NOTIFICATION_PROMOTED_QUEUE = "notification.mentor.promoted.queue";
    public static final String MENTOR_NOTIFICATION_DEMOTED_QUEUE = "notification.mentor.demoted.queue";

    // Review events
    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String REVIEW_NOTIFICATION_SUBMITTED_QUEUE = "notification.review.submitted.queue";

    // Payment event queues and bindings (NEW)
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_NOTIFICATION_SUCCESS_QUEUE = "notification.payment.success.queue";
    public static final String PAYMENT_NOTIFICATION_FAILED_QUEUE = "notification.payment.failed.queue";
    public static final String PAYMENT_NOTIFICATION_COMPENSATED_QUEUE = "notification.payment.compensated.queue";

    // Email retry
    public static final String EMAIL_RETRY_EXCHANGE = "email.retry.exchange";
    public static final String EMAIL_RETRY_QUEUE = "email.retry.queue";
    public static final String EMAIL_DLQ = "email.dlq";

    // Session event queues and bindings (for notification service consumption)
    @Bean public TopicExchange sessionExchange() { return new TopicExchange(SESSION_EXCHANGE, true, false); }
    @Bean public Queue notifSessionRequested() { return QueueBuilder.durable(SESSION_NOTIFICATION_REQUESTED_QUEUE).build(); }
    @Bean public Queue notifSessionAccepted() { return QueueBuilder.durable(SESSION_NOTIFICATION_ACCEPTED_QUEUE).build(); }
    @Bean public Queue notifSessionRejected() { return QueueBuilder.durable(SESSION_NOTIFICATION_REJECTED_QUEUE).build(); }
    @Bean public Queue notifSessionCancelled() { return QueueBuilder.durable(SESSION_NOTIFICATION_CANCELLED_QUEUE).build(); }
    @Bean public Queue notifSessionCompleted() { return QueueBuilder.durable(SESSION_NOTIFICATION_COMPLETED_QUEUE).build(); }

    @Bean public Binding bindSessionRequested() { return BindingBuilder.bind(notifSessionRequested()).to(sessionExchange()).with("session.requested"); }
    @Bean public Binding bindSessionAccepted() { return BindingBuilder.bind(notifSessionAccepted()).to(sessionExchange()).with("session.accepted"); }
    @Bean public Binding bindSessionRejected() { return BindingBuilder.bind(notifSessionRejected()).to(sessionExchange()).with("session.rejected"); }
    @Bean public Binding bindSessionCancelled() { return BindingBuilder.bind(notifSessionCancelled()).to(sessionExchange()).with("session.cancelled"); }
    @Bean public Binding bindSessionCompleted() { return BindingBuilder.bind(notifSessionCompleted()).to(sessionExchange()).with("session.completed"); }

    // Mentor event queues and bindings
    @Bean public TopicExchange mentorExchange() { return new TopicExchange(MENTOR_EXCHANGE, true, false); }
    @Bean public Queue notifMentorApproved() { return QueueBuilder.durable(MENTOR_NOTIFICATION_APPROVED_QUEUE).build(); }
    @Bean public Queue notifMentorRejected() { return QueueBuilder.durable(MENTOR_NOTIFICATION_REJECTED_QUEUE).build(); }
    @Bean public Queue notifMentorPromoted() { return QueueBuilder.durable(MENTOR_NOTIFICATION_PROMOTED_QUEUE).build(); }
    @Bean public Queue notifMentorDemoted() { return QueueBuilder.durable(MENTOR_NOTIFICATION_DEMOTED_QUEUE).build(); }
    @Bean public Binding bindMentorApproved() { return BindingBuilder.bind(notifMentorApproved()).to(mentorExchange()).with("mentor.approved"); }
    @Bean public Binding bindMentorRejected() { return BindingBuilder.bind(notifMentorRejected()).to(mentorExchange()).with("mentor.rejected"); }
    @Bean public Binding bindMentorPromoted() { return BindingBuilder.bind(notifMentorPromoted()).to(mentorExchange()).with("mentor.promoted"); }
    @Bean public Binding bindMentorDemoted() { return BindingBuilder.bind(notifMentorDemoted()).to(mentorExchange()).with("mentor.demoted"); }

    // Review event queues and bindings
    @Bean public TopicExchange reviewExchange() { return new TopicExchange(REVIEW_EXCHANGE, true, false); }
    @Bean public Queue notifReviewSubmitted() { return QueueBuilder.durable(REVIEW_NOTIFICATION_SUBMITTED_QUEUE).build(); }
    @Bean public Binding bindReviewSubmitted() { return BindingBuilder.bind(notifReviewSubmitted()).to(reviewExchange()).with("review.submitted"); }

    // Payment event queues and bindings (NEW)
    @Bean public TopicExchange paymentExchange() { return new TopicExchange(PAYMENT_EXCHANGE, true, false); }
    @Bean public Queue notifPaymentSuccess() { return QueueBuilder.durable(PAYMENT_NOTIFICATION_SUCCESS_QUEUE).build(); }
    @Bean public Queue notifPaymentFailed() { return QueueBuilder.durable(PAYMENT_NOTIFICATION_FAILED_QUEUE).build(); }
    @Bean public Queue notifPaymentCompensated() { return QueueBuilder.durable(PAYMENT_NOTIFICATION_COMPENSATED_QUEUE).build(); }
    @Bean public Binding bindPaymentSuccess() { return BindingBuilder.bind(notifPaymentSuccess()).to(paymentExchange()).with("payment.success"); }
    @Bean public Binding bindPaymentFailed() { return BindingBuilder.bind(notifPaymentFailed()).to(paymentExchange()).with("payment.failed"); }
    @Bean public Binding bindPaymentCompensated() { return BindingBuilder.bind(notifPaymentCompensated()).to(paymentExchange()).with("payment.compensated"); }

    // Email retry queue and dead-letter queue
    @Bean public DirectExchange emailRetryExchange() { return new DirectExchange(EMAIL_RETRY_EXCHANGE, true, false); }
    @Bean public Queue emailRetryQueue() {
        return QueueBuilder.durable(EMAIL_RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ)
                .build();
    }
    @Bean public Queue emailDeadLetterQueue() { return QueueBuilder.durable(EMAIL_DLQ).build(); }
    @Bean public Binding bindEmailRetry() { return BindingBuilder.bind(emailRetryQueue()).to(emailRetryExchange()).with("email.retry"); }

    @Bean public MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}

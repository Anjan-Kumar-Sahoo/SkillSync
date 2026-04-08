package com.skillsync.session.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ==================== SESSION ====================
    public static final String SESSION_EXCHANGE = "session.exchange";
    public static final String SESSION_REQUESTED_QUEUE = "session.requested.queue";
    public static final String SESSION_ACCEPTED_QUEUE = "session.accepted.queue";
    public static final String SESSION_REJECTED_QUEUE = "session.rejected.queue";
    public static final String SESSION_CANCELLED_QUEUE = "session.cancelled.queue";
    public static final String SESSION_COMPLETED_QUEUE = "session.completed.queue";

    @Bean public TopicExchange sessionExchange() { return new TopicExchange(SESSION_EXCHANGE, true, false); }
    @Bean public Queue sessionRequestedQueue() { return QueueBuilder.durable(SESSION_REQUESTED_QUEUE).build(); }
    @Bean public Queue sessionAcceptedQueue() { return QueueBuilder.durable(SESSION_ACCEPTED_QUEUE).build(); }
    @Bean public Queue sessionRejectedQueue() { return QueueBuilder.durable(SESSION_REJECTED_QUEUE).build(); }
    @Bean public Queue sessionCancelledQueue() { return QueueBuilder.durable(SESSION_CANCELLED_QUEUE).build(); }
    @Bean public Queue sessionCompletedQueue() { return QueueBuilder.durable(SESSION_COMPLETED_QUEUE).build(); }

    @Bean public Binding requestedBinding() { return BindingBuilder.bind(sessionRequestedQueue()).to(sessionExchange()).with("session.requested"); }
    @Bean public Binding acceptedBinding() { return BindingBuilder.bind(sessionAcceptedQueue()).to(sessionExchange()).with("session.accepted"); }
    @Bean public Binding rejectedBinding() { return BindingBuilder.bind(sessionRejectedQueue()).to(sessionExchange()).with("session.rejected"); }
    @Bean public Binding cancelledBinding() { return BindingBuilder.bind(sessionCancelledQueue()).to(sessionExchange()).with("session.cancelled"); }
    @Bean public Binding completedBinding() { return BindingBuilder.bind(sessionCompletedQueue()).to(sessionExchange()).with("session.completed"); }

    // ==================== REVIEW ====================
    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String REVIEW_SUBMITTED_QUEUE = "review.submitted.queue";

    @Bean public TopicExchange reviewExchange() { return new TopicExchange(REVIEW_EXCHANGE, true, false); }
    @Bean public Queue reviewSubmittedQueue() { return QueueBuilder.durable(REVIEW_SUBMITTED_QUEUE).build(); }
    @Bean public Binding reviewSubmittedBinding() { return BindingBuilder.bind(reviewSubmittedQueue()).to(reviewExchange()).with("review.submitted"); }

    // ==================== PAYMENT ====================
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String SESSION_PAYMENT_SUCCESS_QUEUE = "session.payment.success.queue";
    public static final String SESSION_PAYMENT_FAILED_QUEUE = "session.payment.failed.queue";
    public static final String SESSION_PAYMENT_COMPENSATED_QUEUE = "session.payment.compensated.queue";

    @Bean public TopicExchange paymentExchange() { return new TopicExchange(PAYMENT_EXCHANGE, true, false); }
    @Bean public Queue sessionPaymentSuccessQueue() { return QueueBuilder.durable(SESSION_PAYMENT_SUCCESS_QUEUE).build(); }
    @Bean public Queue sessionPaymentFailedQueue() { return QueueBuilder.durable(SESSION_PAYMENT_FAILED_QUEUE).build(); }
    @Bean public Queue sessionPaymentCompensatedQueue() { return QueueBuilder.durable(SESSION_PAYMENT_COMPENSATED_QUEUE).build(); }
    @Bean public Binding sessionPaymentSuccessBinding() { return BindingBuilder.bind(sessionPaymentSuccessQueue()).to(paymentExchange()).with("payment.business.action.v1"); }
    @Bean public Binding sessionPaymentFailedBinding() { return BindingBuilder.bind(sessionPaymentFailedQueue()).to(paymentExchange()).with("payment.failed.v1"); }
    @Bean public Binding sessionPaymentCompensatedBinding() { return BindingBuilder.bind(sessionPaymentCompensatedQueue()).to(paymentExchange()).with("payment.compensated.v1"); }

    // ==================== SHARED ====================
    @Bean public MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}

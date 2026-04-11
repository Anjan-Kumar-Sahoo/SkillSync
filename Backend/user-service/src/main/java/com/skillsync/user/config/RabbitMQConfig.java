package com.skillsync.user.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ─── Mentor Events ───
    public static final String MENTOR_EXCHANGE = "mentor.exchange";
    public static final String MENTOR_APPROVED_QUEUE = "mentor.approved.queue";
    public static final String MENTOR_REJECTED_QUEUE = "mentor.rejected.queue";

    // ─── Payment Events (NEW) ───
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.queue";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed.queue";
    public static final String PAYMENT_COMPENSATED_QUEUE = "payment.compensated.queue";

    // ─── Mentor Exchange & Bindings ───

    @Bean
    public TopicExchange mentorExchange() {
        return new TopicExchange(MENTOR_EXCHANGE, true, false);
    }

    @Bean
    public Queue mentorApprovedQueue() {
        return QueueBuilder.durable(MENTOR_APPROVED_QUEUE).build();
    }

    @Bean
    public Queue mentorRejectedQueue() {
        return QueueBuilder.durable(MENTOR_REJECTED_QUEUE).build();
    }

    @Bean
    public Binding mentorApprovedBinding() {
        return BindingBuilder.bind(mentorApprovedQueue()).to(mentorExchange()).with("mentor.approved");
    }

    @Bean
    public Binding mentorRejectedBinding() {
        return BindingBuilder.bind(mentorRejectedQueue()).to(mentorExchange()).with("mentor.rejected");
    }

    // ─── Payment Exchange & Bindings ───

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE).build();
    }

    @Bean
    public Queue paymentCompensatedQueue() {
        return QueueBuilder.durable(PAYMENT_COMPENSATED_QUEUE).build();
    }

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentSuccessQueue()).to(paymentExchange()).with("payment.success");
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue()).to(paymentExchange()).with("payment.failed");
    }

    @Bean
    public Binding paymentCompensatedBinding() {
        return BindingBuilder.bind(paymentCompensatedQueue()).to(paymentExchange()).with("payment.compensated");
    }

    // ─── Payment Business Action Queue (consumed from payment-service) ───
    public static final String PAYMENT_BUSINESS_ACTION_QUEUE = "payment.business.action.queue";

    @Bean
    public Queue paymentBusinessActionQueue() {
        return QueueBuilder.durable(PAYMENT_BUSINESS_ACTION_QUEUE).build();
    }

    @Bean
    public Binding paymentBusinessActionBinding() {
        return BindingBuilder.bind(paymentBusinessActionQueue()).to(paymentExchange()).with("payment.business.action");
    }

    // ─── Message Converter ───

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ─── Review Cache Sync (cross-service) ───
    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String USER_REVIEW_SUBMITTED_QUEUE = "user.review.submitted.queue";

    @Bean
    public TopicExchange reviewExchange() {
        return new TopicExchange(REVIEW_EXCHANGE, true, false);
    }

    @Bean
    public Queue userReviewSubmittedQueue() {
        return QueueBuilder.durable(USER_REVIEW_SUBMITTED_QUEUE).build();
    }

    @Bean
    public Binding userReviewSubmittedBinding() {
        return BindingBuilder.bind(userReviewSubmittedQueue()).to(reviewExchange()).with("review.submitted");
    }

    @Bean
    public Binding userReviewSummaryUpdatedBinding() {
        return BindingBuilder.bind(userReviewSubmittedQueue()).to(reviewExchange()).with("review.summary.updated");
    }
}

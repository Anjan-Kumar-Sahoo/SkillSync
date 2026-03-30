package com.skillsync.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ─── Payment Events ───
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    // ─── Versioned Queues (v1) ───
    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.v1.queue";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed.v1.queue";
    public static final String PAYMENT_COMPENSATED_QUEUE = "payment.compensated.v1.queue";
    public static final String PAYMENT_BUSINESS_ACTION_QUEUE = "payment.business.action.v1.queue";

    // ─── Dead Letter Queues ───
    public static final String DLX_EXCHANGE = "payment.dlx.exchange";
    public static final String DLQ_BUSINESS_ACTION = "payment.business.action.dlq";
    public static final String DLQ_SUCCESS = "payment.success.dlq";
    public static final String DLQ_FAILED = "payment.failed.dlq";

    // ─── Exchanges ───

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    // ─── Main Queues (with DLQ binding) ───

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_SUCCESS)
                .build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_FAILED)
                .build();
    }

    @Bean
    public Queue paymentCompensatedQueue() {
        return QueueBuilder.durable(PAYMENT_COMPENSATED_QUEUE).build();
    }

    @Bean
    public Queue paymentBusinessActionQueue() {
        return QueueBuilder.durable(PAYMENT_BUSINESS_ACTION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_BUSINESS_ACTION)
                .build();
    }

    // ─── Dead Letter Queues ───

    @Bean
    public Queue dlqBusinessAction() {
        return QueueBuilder.durable(DLQ_BUSINESS_ACTION).build();
    }

    @Bean
    public Queue dlqSuccess() {
        return QueueBuilder.durable(DLQ_SUCCESS).build();
    }

    @Bean
    public Queue dlqFailed() {
        return QueueBuilder.durable(DLQ_FAILED).build();
    }

    // ─── Main Queue Bindings (versioned routing keys) ───

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentSuccessQueue()).to(paymentExchange()).with("payment.success.v1");
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue()).to(paymentExchange()).with("payment.failed.v1");
    }

    @Bean
    public Binding paymentCompensatedBinding() {
        return BindingBuilder.bind(paymentCompensatedQueue()).to(paymentExchange()).with("payment.compensated.v1");
    }

    @Bean
    public Binding paymentBusinessActionBinding() {
        return BindingBuilder.bind(paymentBusinessActionQueue()).to(paymentExchange()).with("payment.business.action.v1");
    }

    // ─── DLQ Bindings ───

    @Bean
    public Binding dlqBusinessActionBinding() {
        return BindingBuilder.bind(dlqBusinessAction()).to(deadLetterExchange()).with(DLQ_BUSINESS_ACTION);
    }

    @Bean
    public Binding dlqSuccessBinding() {
        return BindingBuilder.bind(dlqSuccess()).to(deadLetterExchange()).with(DLQ_SUCCESS);
    }

    @Bean
    public Binding dlqFailedBinding() {
        return BindingBuilder.bind(dlqFailed()).to(deadLetterExchange()).with(DLQ_FAILED);
    }

    // ─── Message Converter (schema-evolution safe) ───

    @Bean
    public MessageConverter jsonMessageConverter() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new Jackson2JsonMessageConverter(mapper);
    }
}

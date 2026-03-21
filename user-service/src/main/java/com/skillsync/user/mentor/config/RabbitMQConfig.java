package com.skillsync.user.mentor.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String MENTOR_EXCHANGE = "mentor.exchange";
    public static final String MENTOR_APPROVED_QUEUE = "mentor.approved.queue";
    public static final String MENTOR_REJECTED_QUEUE = "mentor.rejected.queue";

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

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

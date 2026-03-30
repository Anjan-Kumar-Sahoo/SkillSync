package com.skillsync.skill.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SKILL_EXCHANGE = "skill.exchange";
    public static final String SKILL_UPDATED_QUEUE = "skill.updated.queue";
    public static final String SKILL_CREATED_QUEUE = "skill.created.queue";

    @Bean
    public TopicExchange skillExchange() {
        return new TopicExchange(SKILL_EXCHANGE, true, false);
    }

    @Bean
    public Queue skillUpdatedQueue() {
        return QueueBuilder.durable(SKILL_UPDATED_QUEUE).build();
    }

    @Bean
    public Queue skillCreatedQueue() {
        return QueueBuilder.durable(SKILL_CREATED_QUEUE).build();
    }

    @Bean
    public Binding skillUpdatedBinding() {
        return BindingBuilder.bind(skillUpdatedQueue()).to(skillExchange()).with("skill.updated");
    }

    @Bean
    public Binding skillCreatedBinding() {
        return BindingBuilder.bind(skillCreatedQueue()).to(skillExchange()).with("skill.created");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

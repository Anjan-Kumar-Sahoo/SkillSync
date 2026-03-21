package com.skillsync.session.review.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReviewRabbitMQConfig {
    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String REVIEW_SUBMITTED_QUEUE = "review.submitted.queue";

    @Bean public TopicExchange reviewExchange() { return new TopicExchange(REVIEW_EXCHANGE, true, false); }
    @Bean public Queue reviewSubmittedQueue() { return QueueBuilder.durable(REVIEW_SUBMITTED_QUEUE).build(); }
    @Bean public Binding reviewSubmittedBinding() { return BindingBuilder.bind(reviewSubmittedQueue()).to(reviewExchange()).with("review.submitted"); }
}

//package com.skillsync.sessionservice.config;
//
//import org.springframework.amqp.core.*;
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.amqp.support.converter.MessageConverter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RabbitMQConfig {
//
//    public static final String SESSION_EXCHANGE    = "session.exchange";
//    public static final String SESSION_BOOKED_QUEUE  = "session.booked.queue";
//    public static final String SESSION_BOOKED_KEY    = "session.booked";
//
//    // Declare exchange
//    @Bean
//    public TopicExchange sessionExchange() {
//        return new TopicExchange(SESSION_EXCHANGE);
//    }
//
//    // Declare queue
//    @Bean
//    public Queue sessionBookedQueue() {
//        return QueueBuilder.durable(SESSION_BOOKED_QUEUE).build();
//    }
//
//    // Bind queue to exchange with routing key
//    @Bean
//    public Binding sessionBookedBinding(Queue sessionBookedQueue, TopicExchange sessionExchange) {
//        return BindingBuilder
//                .bind(sessionBookedQueue)
//                .to(sessionExchange)
//                .with(SESSION_BOOKED_KEY);
//    }
//
//    // Use JSON converter so messages are human-readable
//    @Bean
//    public MessageConverter jsonMessageConverter() {
//        return new Jackson2JsonMessageConverter();
//    }
//
//    // Plug the JSON converter into the RabbitTemplate
//    @Bean
//    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//        RabbitTemplate template = new RabbitTemplate(connectionFactory);
//        template.setMessageConverter(jsonMessageConverter());
//        return template;
//    }
//}

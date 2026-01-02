package org.example.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {

    public static final String EVENTS_QUEUE = "ticketManagement_events_queue";

    public static final String EVENTS_EXCHANGE = "events_exchange";

    public static final String EXCHANGE = "ticketManagement_exchange";

    public static final String ROUTING_KEY = "ticketManagement";


    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE);
    }

    @Bean
    public Queue ticketManagementEventsQueue() {
        return new Queue(EVENTS_QUEUE, true);
    }

    @Bean
    public Binding ticketManagementTicketsBinding(
            @Qualifier("ticketManagementEventsQueue") Queue queue,
            @Qualifier("eventsExchange") TopicExchange exchange) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("events");
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

}

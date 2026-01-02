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

    public static final String TICKETS_QUEUE = "fileGeneration_tickets_queue";

    public static final String TICKETS_EXCHANGE = "tickets_exchange";

    public static final String FILES_EXCHANGE = "files_exchange";

    public static final String FILES_ROUTING_KEY = "files";

    @Bean
    public TopicExchange ticketsExchange() {
        return new TopicExchange(TICKETS_EXCHANGE);
    }

    @Bean
    public Queue fileGenerationTicketsQueue() {
        return new Queue(TICKETS_QUEUE, true);
    }

    @Bean
    public Binding fileGenerationTicketsBinding(
            @Qualifier("fileGenerationTicketsQueue") Queue queue,
            @Qualifier("ticketsExchange") TopicExchange exchange) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("tickets.created");
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(FILES_EXCHANGE);
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

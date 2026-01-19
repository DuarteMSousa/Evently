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

    public static final String QUEUE = "events_queue";

    public static final String EVENTS_EXCHANGE = "events_exchange";

    public static final String EVENTS_ROUTING_KEY = "events";

    public static final String TICKET_MANAGEMENT_STOCK_GENERATED_QUEUE = "events_ticket_management_stock_generated_queue";

    public static final String TICKET_MANAGEMENT_STOCK_FAILED_QUEUE = "events_ticket_management_stock_failed_queue";

    public static final String TICKET_MANAGEMENT_EXCHANGE = "ticketManagement_exchange";

    @Bean
    public TopicExchange ticketManagementExchange() {
        return new TopicExchange(TICKET_MANAGEMENT_EXCHANGE);
    }
    //stock generated
    @Bean
    public Queue eventsTicketManagementStockGeneratedQueue() {
        return new Queue(TICKET_MANAGEMENT_STOCK_GENERATED_QUEUE, true);
    }

    @Bean
    public Binding ticketManagementStockGeneratedTicketsBinding(
            @Qualifier("eventsTicketManagementStockGeneratedQueue") Queue queue,
            @Qualifier("ticketManagementExchange") TopicExchange exchange) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("ticketManagement.stock.#");
    }

    //stock failed
    @Bean
    public Queue eventsTicketManagementStockFailedQueue() {
        return new Queue(TICKET_MANAGEMENT_STOCK_FAILED_QUEUE, true);
    }

    @Bean
    public Binding ticketManagementStockFailedTicketsBinding(
            @Qualifier("eventsTicketManagementStockFailedQueue") Queue queue,
            @Qualifier("ticketManagementExchange") TopicExchange exchange) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("ticketManagement.stock.#");
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

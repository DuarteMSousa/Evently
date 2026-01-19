package org.evently.tickets.config;

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

    public static final String TICKET_MANAGEMENT_QUEUE = "tickets_ticketManagement_queue";

    public static final String TICKET_MANAGEMENT_EXCHANGE = "ticketManagement_exchange";

    public static final String REFUNDS_QUEUE = "refunds_queue";

    public static final String REFUNDS_EXCHANGE = "refunds_exchange";

    public static final String ORDERS_QUEUE = "orders_queue";

    public static final String ORDERS_EXCHANGE = "orders_exchange";

    public static final String EXCHANGE = "tickets_exchange";

    public static final String ROUTING_KEY = "tickets";


    @Bean
    public TopicExchange ticketManagementExchange() {
        return new TopicExchange(TICKET_MANAGEMENT_EXCHANGE);
    }

    @Bean
    public Queue ticketsTicketManagementQueue() {
        return new Queue(TICKET_MANAGEMENT_QUEUE);
    }

    @Bean
    public Binding ticketsTicketManagementBinding(
            @Qualifier("ticketsTicketManagementQueue") Queue queue,
            @Qualifier("ticketManagementExchange") TopicExchange exchange) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("ticketManagement.reservation.confirmed");
    }

    @Bean
    public TopicExchange refundsExchange() {
        return new TopicExchange(REFUNDS_EXCHANGE);
    }

    @Bean
    public Queue ticketsRefundsQueue() {
        return new Queue(REFUNDS_QUEUE);
    }

    @Bean
    public Binding ticketsRefundsBinding(
            @Qualifier("ticketsRefundsQueue") Queue queue,
            @Qualifier("refundsExchange") TopicExchange exchange) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("refunds.decision.registered");
    }

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE);
    }

    @Bean
    public Queue ticketsOrdersQueue() {
        return new Queue(ORDERS_QUEUE);
    }

    @Bean
    public Binding ticketsOrdersBinding(
            @Qualifier("ticketsOrdersQueue") Queue queue,
            @Qualifier("ordersExchange") TopicExchange exchange) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("orders.canceled");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

}

package org.evently.config;

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

    public static final String PAYMENTS_QUEUE = "refunds_payments_queue";

    public static final String PAYMENTS_EXCHANGE = "payments_exchange";

    public static final String EXCHANGE = "refunds_exchange";

    public static final String RK_REFUND_REQUEST_MESSAGE_SENT = "refunds.request.sent";
    public static final String RK_REFUND_DECISION_REGISTERED  = "refunds.decision.registered";

    @Bean
    public TopicExchange paymentsExchange() {
        return new TopicExchange(PAYMENTS_EXCHANGE);
    }

    @Bean
    public Queue refundsPaymentsQueue() {
        return new Queue(PAYMENTS_QUEUE);
    }

    @Bean
    public Binding refundsPayementsBinding(
            @Qualifier("refundsPaymentsQueue") Queue queue,
            @Qualifier("paymentsExchange") TopicExchange exchange) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("payments.*");
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

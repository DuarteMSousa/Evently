package org.example.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.payments.exchange}")
    private String paymentsExchangeName;

    @Value("${app.payments.queue}")
    private String paymentsQueueName;

    @Value("${app.payments.routing-key}")
    private String paymentsRoutingKey;

    @Bean
    public TopicExchange paymentsExchange() {
        return new TopicExchange(paymentsExchangeName);
    }

    @Bean
    public Queue paymentsQueue() {
        return new Queue(paymentsQueueName, true);
    }

    @Bean
    public Binding paymentsBinding(Queue paymentsQueue, TopicExchange paymentsExchange) {
        return BindingBuilder
                .bind(paymentsQueue)
                .to(paymentsExchange)
                .with(paymentsRoutingKey);
    }
}

package org.example.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
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

    // --- topologia (ok) ---
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

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }

    @Bean
    public TopicExchange refundsExchange(@Value("${app.refunds.exchange}") String name) {
        return new TopicExchange(name);
    }

    @Bean
    public Queue refundsQueue(@Value("${app.refunds.queue}") String name) {
        return new Queue(name, true);
    }

    @Bean
    public Binding refundsBinding(
            Queue refundsQueue,
            TopicExchange refundsExchange,
            @Value("${app.refunds.routing-key}") String routingKey) {

        return BindingBuilder
                .bind(refundsQueue)
                .to(refundsExchange)
                .with(routingKey);
    }
}

package org.example.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.payments.exchange}")
    private String paymentsExchangeName;

    public static final String paymentsRefundsQueueName="payments_refunds";

    public static final String refundsExchangeName="refunds_exchange";

    public static final String paymentsOrdersQueueName="payments_orders";

    public static final String ordersExchangeName="orders_exchange";

    @Value("${app.payments.routing-key}")
    private String paymentsRoutingKey;

    // --- topologia (ok) ---
    @Bean
    public TopicExchange paymentsExchange() {
        return new TopicExchange(paymentsExchangeName);
    }

    //REFUNDS
    @Bean
    public TopicExchange refundsExchange() {
        return new TopicExchange(refundsExchangeName);
    }

    @Bean
    public Queue paymentsRefundsQueue() {
        return new Queue(paymentsRefundsQueueName, true);
    }

    @Bean
    public Binding paymentsRefundsBinding(
            @Qualifier("paymentsRefundsQueue") Queue queue,
            @Qualifier("refundsExchange") TopicExchange exchange) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("refunds.accepted");
    }

    //ORDERS
    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ordersExchangeName);
    }

    @Bean
    public Queue paymentsOrdersQueue() {
        return new Queue(paymentsOrdersQueueName, true);
    }

    @Bean
    public Binding paymentsOrdersBinding(
            @Qualifier("paymentsOrdersQueue") Queue queue,
            @Qualifier("ordersExchange") TopicExchange exchange) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with("orders.created");
    }

    //CONFIGS
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


}

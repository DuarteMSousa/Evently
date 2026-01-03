package org.example.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    public static final String PAYMENTS_EXCHANGE = "payments_exchange";
    public static final String REFUNDS_EXCHANGE  = "refunds_exchange";
    public static final String FILES_EXCHANGE    = "fileGeneration_exchange";

    // Queues
    public static final String NOTIF_PAYMENTS_QUEUE = "notifications_payments";
    public static final String NOTIF_REFUND_REQUESTS_QUEUE  = "notifications_refunds_requests";
    public static final String NOTIF_REFUND_DECISIONS_QUEUE = "notifications_refunds_decisions";
    public static final String NOTIF_FILES_QUEUE    = "notifications_files";

    // Exchanges
    @Bean
    public TopicExchange paymentsExchange() { return new TopicExchange(PAYMENTS_EXCHANGE); }

    @Bean
    public TopicExchange refundsExchange() { return new TopicExchange(REFUNDS_EXCHANGE); }

    @Bean
    public TopicExchange filesExchange() { return new TopicExchange(FILES_EXCHANGE); }

    // Queues
    @Bean
    public Queue notificationsPaymentsQueue() { return new Queue(NOTIF_PAYMENTS_QUEUE, true); }

    @Bean
    public Queue notificationsRefundRequestsQueue() {
        return new Queue(NOTIF_REFUND_REQUESTS_QUEUE, true);
    }

    @Bean
    public Queue notificationsRefundDecisionsQueue() {
        return new Queue(NOTIF_REFUND_DECISIONS_QUEUE, true);
    }

    @Bean
    public Queue notificationsFilesQueue() { return new Queue(NOTIF_FILES_QUEUE, true); }

    // Bindings
    @Bean
    public Binding bindPayments(Queue notificationsPaymentsQueue, TopicExchange paymentsExchange) {
        return BindingBuilder.bind(notificationsPaymentsQueue).to(paymentsExchange).with("payments.*");
    }

    @Bean
    public Binding bindRefundRequests(Queue notificationsRefundRequestsQueue,
                                      TopicExchange refundsExchange) {
        return BindingBuilder.bind(notificationsRefundRequestsQueue)
                .to(refundsExchange)
                .with("refunds.request.sent");
    }

    @Bean
    public Binding bindRefundDecisions(Queue notificationsRefundDecisionsQueue,
                                       TopicExchange refundsExchange) {
        return BindingBuilder.bind(notificationsRefundDecisionsQueue)
                .to(refundsExchange)
                .with("refunds.decision.registered");
    }

    @Bean
    public Binding bindFiles(Queue notificationsFilesQueue, TopicExchange filesExchange) {
        return BindingBuilder.bind(notificationsFilesQueue).to(filesExchange).with("files.*");
    }

    // JSON
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
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
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}

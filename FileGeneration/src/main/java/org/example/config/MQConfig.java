package org.example.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {

    public static final String TICKETS_QUEUE = "tickets_queue";

    public static final String FILES_QUEUE = "files_queue";

    public static final String FILES_EXCHANGE = "files_exchange";

    public static final String FILES_ROUTING_KEY = "files_routing_key";


    @Bean
    public Queue queue() {
        return new Queue(FILES_QUEUE, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(FILES_EXCHANGE);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(FILES_ROUTING_KEY);
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

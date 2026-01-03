package org.example.listeners;

import org.example.config.MQConfig;
import org.example.messages.received.OrderPayedMessage;
import org.example.services.TicketStocksService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessagesListener {

    @Autowired
    private TicketStocksService ticketStocksService;

    @Autowired
    private RabbitTemplate template;

    @RabbitListener(queues = MQConfig.ORDERS_QUEUE)
    public void listener(OrderPayedMessage message) {


    }
}

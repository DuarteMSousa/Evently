package org.example.listeners;

import org.example.config.MQConfig;
import org.example.messages.received.OrderPaidMessage;
import org.example.publishers.TicketManagementMessagesPublisher;
import org.example.services.TicketReservationsService;
import org.example.services.TicketStocksService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessagesListener {

    @Autowired
    private TicketReservationsService ticketReservationsService;

    @Autowired
    private RabbitTemplate template;

    @RabbitListener(queues = MQConfig.ORDERS_QUEUE)
    public void listener(OrderPaidMessage message) {
        ticketReservationsService.handleOrderPaid(message);


    }
}

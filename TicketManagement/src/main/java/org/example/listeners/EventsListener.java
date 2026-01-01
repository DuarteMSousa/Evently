package org.example.listeners;

import org.example.config.MQConfig;
import org.example.messages.received.EventUpdatedMessage;
import org.example.services.TicketStocksService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventsListener {

    @Autowired
    private TicketStocksService ticketStocksService;

    @Autowired
    private RabbitTemplate template;

    @RabbitListener(queues = MQConfig.EVENT_QUEUE)
    public void listener(EventUpdatedMessage event) {


    }
}

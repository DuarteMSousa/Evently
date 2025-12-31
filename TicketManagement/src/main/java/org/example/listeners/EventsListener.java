package org.example.listeners;

import org.example.config.MQConfig;
import org.example.dtos.externalServices.eventSessions.EventSessionDTO;
import org.example.dtos.externalServices.sessionTiers.SessionTierDTO;
import org.example.enums.externalServices.EventStatus;
import org.example.events.TicketStockGeneratedEvent;
import org.example.events.received.EventUpdatedEvent;
import org.example.exceptions.TicketStockNotFoundException;
import org.example.models.TicketStock;
import org.example.models.TicketStockId;
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
    public void listener(EventUpdatedEvent event) {


    }
}

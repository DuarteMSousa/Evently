package org.example.listeners;

import org.example.config.MQConfig;
import org.example.messages.received.EventTicketStockGeneratedMessage;
import org.example.messages.received.EventTicketStockGenerationFailedMessage;
import org.example.services.EventsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TicketManagementMessagesListener {

    @Autowired
    private EventsService eventsService;

    @RabbitListener(queues = MQConfig.TICKET_MANAGEMENT_STOCK_GENERATED_QUEUE)
    public void listener(EventTicketStockGeneratedMessage message) {
        eventsService.handleEventStockGeneratedMessage(message.getEventId());
    }

    @RabbitListener(queues = MQConfig.TICKET_MANAGEMENT_STOCK_FAILED_QUEUE)
    public void listener(EventTicketStockGenerationFailedMessage message) {
        eventsService.handleEventStockGenerationFailedMessage(message.getEventId());
    }

}

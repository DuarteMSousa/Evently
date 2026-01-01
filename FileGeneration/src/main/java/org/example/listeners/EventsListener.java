package org.example.listeners;

import org.example.config.MQConfig;
import org.example.messages.TicketGeneratedMessage;
import org.example.services.TicketFileGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventsListener {

    @Autowired
    private TicketFileGenerationService ticketFileGenerationService;

    private Logger logger = LoggerFactory.getLogger(EventsListener.class);

    private static final Marker TICKET_GENERATED_HANDLE = MarkerFactory.getMarker("TICKET_GENERATED_HANDLE");

    @RabbitListener(queues = MQConfig.TICKETS_QUEUE)
    public void listener(TicketGeneratedMessage event) {
        logger.info(TICKET_GENERATED_HANDLE, "TicketGeneratedEvent received");
        try {
            ticketFileGenerationService.saveTicketFile(event);
        } catch (Exception e) {
            logger.info(TICKET_GENERATED_HANDLE, "Exception caught while saving ticket file: {}", e.getMessage());
        }
    }
}

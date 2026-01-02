package org.evently.tickets.publishers;

import org.evently.tickets.config.MQConfig;
import org.evently.tickets.messages.TicketIssuedMessage;
import org.evently.tickets.models.Ticket;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class TicketsEventsPublisher {
    private final RabbitTemplate rabbitTemplate;

    public TicketsEventsPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTicketIssuedEvent(Ticket ticket) {
        TicketIssuedMessage ticketIssuedMessage = new TicketIssuedMessage();
        ticketIssuedMessage.setId(ticket.getId());
        ticketIssuedMessage.setReservationId(ticket.getReservationId());
        ticketIssuedMessage.setOrderId(ticket.getOrderId());
        ticketIssuedMessage.setUserId(ticket.getUserId());
        ticketIssuedMessage.setEventId(ticket.getEventId());
        ticketIssuedMessage.setSessionId(ticket.getSessionId());
        ticketIssuedMessage.setTierId(ticket.getTierId());

        rabbitTemplate.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY, ticketIssuedMessage);
    }
}

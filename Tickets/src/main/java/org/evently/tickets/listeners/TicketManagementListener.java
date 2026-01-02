package org.evently.tickets.listeners;

import org.evently.tickets.config.MQConfig;
import org.evently.tickets.enums.TicketStatus;
import org.evently.tickets.messages.received.TicketReservationConfirmedMessage;
import org.evently.tickets.models.Ticket;
import org.evently.tickets.services.TicketsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TicketManagementListener {

    @Autowired
    private TicketsService ticketsService;

    @Autowired
    private RabbitTemplate template;

    @RabbitListener(queues = MQConfig.TICKET_MANAGEMENT_QUEUE)
    public void listener(TicketReservationConfirmedMessage event) {
        Ticket ticket = new Ticket();
        ticket.setReservationId(event.getId());
        ticket.setOrderId(event.getOrderId());
        ticket.setUserId(event.getUserId());
        ticket.setEventId(event.getEventId());
        ticket.setSessionId(event.getSessionId());
        ticket.setTierId(event.getTierId());
        ticket.setStatus(TicketStatus.ISSUED);
        ticket.setIssuedAt(new Date());

        ticketsService.issueTicket(ticket);

    }
}

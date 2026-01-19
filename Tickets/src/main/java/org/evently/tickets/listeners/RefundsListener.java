package org.evently.tickets.listeners;

import org.evently.tickets.config.MQConfig;
import org.evently.tickets.enums.externalServices.DecisionType;
import org.evently.tickets.messages.received.RefundRequestDecisionRegisteredMessage;
import org.evently.tickets.models.Ticket;
import org.evently.tickets.services.TicketsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RefundsListener {

    @Autowired
    private TicketsService ticketsService;

    @Autowired
    private RabbitTemplate template;

    @RabbitListener(queues = MQConfig.REFUNDS_QUEUE)
    public void listener(RefundRequestDecisionRegisteredMessage refundRequestDecision) {
        if (refundRequestDecision.getDecisionType() == DecisionType.APPROVE) {
            List<Ticket> orderTickets = ticketsService.findAllByOrderId(refundRequestDecision.getOrderId());

            orderTickets.forEach(orderTicket -> {
                ticketsService.cancelTicket(orderTicket.getId());
            });
        }
    }
}

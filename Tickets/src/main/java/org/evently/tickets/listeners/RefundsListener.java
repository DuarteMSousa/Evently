package org.evently.tickets.listeners;

import org.evently.tickets.config.MQConfig;
import org.evently.tickets.enums.externalServices.DecisionType;
import org.evently.tickets.exceptions.InvalidTicketUpdateException;
import org.evently.tickets.messages.received.RefundRequestDecisionRegisteredMessage;
import org.evently.tickets.models.Ticket;
import org.evently.tickets.services.TicketsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RefundsListener {

    @Autowired
    private TicketsService ticketsService;

    private static final Logger logger = LoggerFactory.getLogger(RefundsListener.class);

    @RabbitListener(queues = MQConfig.REFUNDS_QUEUE)
    public void listener(RefundRequestDecisionRegisteredMessage refundRequestDecision) {
        if (refundRequestDecision.getDecisionType() == DecisionType.APPROVE) {
            List<Ticket> orderTickets = ticketsService.findAllByOrderId(refundRequestDecision.getOrderId());

            orderTickets.forEach(orderTicket -> {
                try{
                    ticketsService.cancelTicket(orderTicket.getId());
                } catch(InvalidTicketUpdateException e) {
                    logger.info("Ticket already canceled");
                }
            });
        }
    }

}

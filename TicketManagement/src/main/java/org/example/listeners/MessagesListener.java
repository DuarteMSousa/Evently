package org.example.listeners;

import org.example.config.MQConfig;
import org.example.enums.externalServices.DecisionType;
import org.example.messages.received.OrderCanceledMessage;
import org.example.messages.received.OrderPaidMessage;
import org.example.messages.received.RefundRequestDecisionRegisteredMessage;
import org.example.services.TicketReservationsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessagesListener {

    @Autowired
    private TicketReservationsService ticketReservationsService;

    @Autowired
    private RabbitTemplate template;

    @RabbitListener(queues = MQConfig.ORDERS_PAID_QUEUE)
    public void listener(OrderPaidMessage message) {
        ticketReservationsService.handleOrderPaid(message);
    }

    @RabbitListener(queues = MQConfig.ORDERS_CANCELED_QUEUE)
    public void listener(OrderCanceledMessage message) {
        ticketReservationsService.handleOrderCanceled(message);
    }

    @RabbitListener(queues = MQConfig.REFUNDS_QUEUE)
    public void listener(RefundRequestDecisionRegisteredMessage message) {
        if (message.getDecisionType().equals(DecisionType.APPROVE)) {
            ticketReservationsService.handleRefundRequestDecision(message);
        }
    }
}

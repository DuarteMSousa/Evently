package org.evently.orders.listeners;

import org.evently.orders.config.MQConfig;
import org.evently.orders.enums.externalServices.DecisionType;
import org.evently.orders.messages.received.RefundRequestDecisionRegisteredMessage;
import org.evently.orders.services.OrdersService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RefundsListener {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private RabbitTemplate template;

    @RabbitListener(queues = MQConfig.REFUNDS_QUEUE)
    public void listener(RefundRequestDecisionRegisteredMessage refundRequestDecision) {
        if (refundRequestDecision.getDecisionType() == DecisionType.APPROVE) {
            ordersService.cancelOrder(refundRequestDecision.getOrderId());
        }
    }
}

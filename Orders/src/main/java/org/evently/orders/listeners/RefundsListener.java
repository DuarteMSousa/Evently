package org.evently.orders.listeners;

import org.evently.orders.config.MQConfig;
import org.evently.orders.enums.externalServices.refunds.DecisionType;
import org.evently.orders.messages.received.RefundRequestDecisionRegisteredMessage;
import org.evently.orders.services.OrdersService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RefundsListener {

    @Autowired
    private OrdersService ordersService;

    @RabbitListener(queues = MQConfig.REFUNDS_QUEUE)
    public void listener(RefundRequestDecisionRegisteredMessage refundRequestDecision) {
        if (refundRequestDecision.getDecisionType() == DecisionType.APPROVE) {
            ordersService.cancelOrder(refundRequestDecision.getOrderId());
        }
    }

}

package org.evently.orders.listeners;

import org.evently.orders.config.MQConfig;
import org.evently.orders.enums.externalServices.payments.PaymentEventType;
import org.evently.orders.enums.externalServices.payments.PaymentStatus;
import org.evently.orders.messages.received.PaymentEventMessage;
import org.evently.orders.models.Order;
import org.evently.orders.services.OrdersService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentsListener {

    @Autowired
    private OrdersService ordersService;

    @RabbitListener(queues = MQConfig.PAYMENTS_QUEUE)
    public void listener(PaymentEventMessage payment) {
        Order order = ordersService.getOrder(payment.getOrderId());

        if (order != null) {
            if (payment.getEventType() == PaymentEventType.CAPTURED && payment.getStatus() == PaymentStatus.CAPTURED) {
                ordersService.markAsPaid(order.getId());
            } else if (payment.getEventType() == PaymentEventType.FAILED && payment.getStatus() == PaymentStatus.FAILED) {
                ordersService.markAsPaymentFailed(order.getId());
            }
        }
    }

}

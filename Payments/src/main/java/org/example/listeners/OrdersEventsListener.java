package org.example.listeners;

import org.example.config.RabbitMQConfig;
import org.example.messages.externalServices.OrderCreatedMessage;
import org.example.services.PaymentsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrdersEventsListener {

    private final PaymentsService paymentsService;

    public OrdersEventsListener(PaymentsService paymentsService) {
        this.paymentsService = paymentsService;
    }

    @RabbitListener(
            queues = RabbitMQConfig.paymentsOrdersQueueName,
            containerFactory = "rabbitListenerContainerFactory"
    )

    public void handleOrderCreated(OrderCreatedMessage message) {
        paymentsService.onOrderCreated(message.getId(), message.getUserId(), message.getTotal());
    }

}

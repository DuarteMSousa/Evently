package org.example.listeners;

import org.example.config.RabbitMQConfig;
import org.example.messages.externalServices.PaymentEventMessage;
import org.example.service.NotificationsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentsEventsListener {

    private final NotificationsService notificationsService;

    public PaymentsEventsListener(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }

    @RabbitListener(
            queues = RabbitMQConfig.NOTIF_PAYMENTS_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePaymentEvent(PaymentEventMessage msg) {

        if ("CAPTURED".equalsIgnoreCase(msg.getEventType())) {
            notificationsService.notifyPaymentCaptured(msg.getUserId(), msg.getOrderId(), msg.getAmount());
        }

        if ("FAILED".equalsIgnoreCase(msg.getEventType())) {
            notificationsService.notifyPaymentFailed(msg.getUserId(), msg.getOrderId(), msg.getAmount());
        }

        if ("REFUND".equalsIgnoreCase(msg.getEventType()) || "REFUNDED".equalsIgnoreCase(msg.getEventType())) {
            notificationsService.notifyPaymentRefunded(msg.getUserId(), msg.getOrderId(), msg.getAmount());
        }
    }
}
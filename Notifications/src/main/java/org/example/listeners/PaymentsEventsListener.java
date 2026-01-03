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

        switch (msg.getEventType()) {

            case CAPTURED:
                notificationsService.notifyPaymentCaptured(
                        msg.getUserId(),
                        msg.getOrderId(),
                        msg.getAmount()
                );
                break;

            case FAILED:
                notificationsService.notifyPaymentFailed(
                        msg.getUserId(),
                        msg.getOrderId(),
                        msg.getAmount()
                );
                break;

            case REFUND:
                notificationsService.notifyPaymentRefunded(
                        msg.getUserId(),
                        msg.getOrderId(),
                        msg.getAmount()
                );
                break;

            default:
                break;
        }
    }
}
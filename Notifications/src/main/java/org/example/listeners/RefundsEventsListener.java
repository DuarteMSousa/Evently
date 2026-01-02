package org.example.listeners;

import org.example.config.RabbitMQConfig;
import org.example.messages.externalServices.RefundEventMessage;
import org.example.service.NotificationsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RefundsEventsListener {

    private final NotificationsService notificationsService;

    public RefundsEventsListener(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }

    @RabbitListener(
            queues = RabbitMQConfig.NOTIF_REFUNDS_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleRefundEvent(RefundEventMessage msg) {

        if ("REFUND_PROCESSED".equalsIgnoreCase(msg.getEventType())) {
            // aqui não tens userId no evento -> precisas de obter via paymentId (ver nota abaixo)
            notificationsService.notifyRefundProcessed(msg.getPaymentId(), msg.getRefundId(), msg.getAmount());
        }

        if ("REFUND_FAILED".equalsIgnoreCase(msg.getEventType())) {
            notificationsService.notifyRefundFailed(msg.getPaymentId(), msg.getRefundId());
        }
    }
}
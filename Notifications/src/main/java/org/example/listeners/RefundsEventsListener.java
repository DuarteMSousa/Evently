package org.example.listeners;

import org.example.config.RabbitMQConfig;
import org.example.messages.externalServices.RefundRequestDecisionRegisteredMessage;
import org.example.messages.externalServices.RefundRequestMessageSentMessage;
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
            queues = RabbitMQConfig.NOTIF_REFUND_REQUESTS_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleRefundRequestSent(RefundRequestMessageSentMessage msg) {
        notificationsService.notifyRefundRequestSent(
                msg.getUserId(),
                msg.getRefundRequestId(),
                msg.getContent()
        );
    }

    @RabbitListener(
            queues = RabbitMQConfig.NOTIF_REFUND_DECISIONS_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleRefundDecision(RefundRequestDecisionRegisteredMessage msg) {
        notificationsService.notifyRefundDecision(
                msg.getUserToRefundId(),
                msg.getPaymentId(),
                msg.getDecisionType(),
                msg.getDescription()
        );
    }
}

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
            queues = RabbitMQConfig.NOTIF_REFUNDS_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleRefundRequestMessageSent(RefundRequestMessageSentMessage msg) {
        notificationsService.notifyRefundRequestSent(
                msg.getUserId(),
                msg.getRefundRequestId(),
                msg.getContent()
        );
    }

    @RabbitListener(
            queues = RabbitMQConfig.NOTIF_REFUNDS_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleRefundDecisionRegistered(RefundRequestDecisionRegisteredMessage msg) {
        notificationsService.notifyRefundDecision(
                msg.getUserToRefundId(),
                msg.getPaymentId(),
                msg.getDecisionType(),
                msg.getDescription()
        );
    }
}

/*
package org.example.listeners;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.config.RabbitMQConfig;
import org.example.service.NotificationsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
    public void handleRefundEvent(JsonNode msg) {

        // formato: RefundRequestMessageSentMessage
        if (msg.hasNonNull("refundRequestId") && msg.hasNonNull("userId") && msg.hasNonNull("content")) {
            notificationsService.notifyRefundRequestSent(
                    UUID.fromString(msg.get("userId").asText()),
                    UUID.fromString(msg.get("refundRequestId").asText()),
                    msg.get("content").asText()
            );
            return;
        }

        // formato: RefundRequestDecisionRegisteredMessage
        if (msg.hasNonNull("userToRefundId") && msg.hasNonNull("paymentId") && msg.hasNonNull("decisionType")) {
            notificationsService.notifyRefundDecision(
                    UUID.fromString(msg.get("userToRefundId").asText()),
                    UUID.fromString(msg.get("paymentId").asText()),
                    msg.get("decisionType").asText(),
                    msg.hasNonNull("description") ? msg.get("description").asText() : null
            );
        }
    }
}

 */
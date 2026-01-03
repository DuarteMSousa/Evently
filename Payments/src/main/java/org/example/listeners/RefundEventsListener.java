package org.example.listeners;

import org.example.config.RabbitMQConfig;
import org.example.messages.externalServices.RefundRequestDecisionRegisteredMessage;
import org.example.services.PaymentsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RefundDecisionsListener {

    private final PaymentsService paymentsService;

    public RefundDecisionsListener(PaymentsService paymentsService) {
        this.paymentsService = paymentsService;
    }

    @RabbitListener(
            queues = RabbitMQConfig.paymentsRefundsQueueName,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleRefundDecision(RefundRequestDecisionRegisteredMessage msg) {

        String dt = msg.getDecisionType();

        if ("ACCEPTED".equalsIgnoreCase(dt) || "APPROVED".equalsIgnoreCase(dt)) {
            paymentsService.onRefundApproved(msg.getPaymentId());
            return;
        }

        if ("REJECTED".equalsIgnoreCase(dt) || "DENIED".equalsIgnoreCase(dt)) {
            paymentsService.onRefundRejected(msg.getPaymentId(), msg.getDescription());
        }
    }
}
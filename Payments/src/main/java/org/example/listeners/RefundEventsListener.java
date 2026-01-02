package org.example.listeners;

import org.example.config.RabbitMQConfig;
import org.example.dtos.externalservice.RefundEventMessage;
import org.example.services.PaymentsService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RefundEventsListener {

    private final PaymentsService paymentsService;

    public RefundEventsListener(PaymentsService paymentsService) {
        this.paymentsService = paymentsService;
    }

    @RabbitListener(
            queues = RabbitMQConfig.paymentsRefundsQueueName,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleRefundEvent(RefundEventMessage message) {
        // exemplo de lógica:
        // se refund foi processado -> atualizar Payment para REFUNDED
        // se falhou -> talvez marcar algo como REFUND_FAILED / manter CAPTURED

        if ("REFUND_PROCESSED".equalsIgnoreCase(message.getEventType())) {
            paymentsService.onRefundProcessed(message.getPaymentId(), message.getAmount(), message.getRefundId());
        }

        if ("REFUND_FAILED".equalsIgnoreCase(message.getEventType())) {
            paymentsService.onRefundFailed(message.getPaymentId(), message.getRefundId());
        }
    }
}

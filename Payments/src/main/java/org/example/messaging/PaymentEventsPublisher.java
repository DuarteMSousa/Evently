package org.example.messaging;

import org.example.models.Payment;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentEventsPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.payments.exchange}")
    private String paymentsExchangeName;

    @Value("${app.payments.routing-key}")
    private String paymentsRoutingKey;

    public PaymentEventsPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentEvent(String eventType, Payment payment) {
        PaymentEventMessage message = new PaymentEventMessage(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                eventType
        );

        // ✅ aqui estava o teu bug: exchange/routingKey não existiam
        rabbitTemplate.convertAndSend(paymentsExchangeName, paymentsRoutingKey, message);
    }

    /**
     * Mensagem publicada no broker.
     *
     * Nota: NÃO usar Serializable para microserviços.
     * Com Jackson2JsonMessageConverter, isto vai em JSON.
     */
    public static class PaymentEventMessage {

        private UUID paymentId;
        private UUID orderId;
        private UUID userId;
        private BigDecimal amount;
        private String status;
        private String eventType;

        public PaymentEventMessage(UUID paymentId,
                                   UUID orderId,
                                   UUID userId,
                                   BigDecimal amount,
                                   String status,
                                   String eventType) {
            this.paymentId = paymentId;
            this.orderId = orderId;
            this.userId = userId;
            this.amount = amount;
            this.status = status;
            this.eventType = eventType;
        }

        public PaymentEventMessage() {
        }

        public UUID getPaymentId() { return paymentId; }
        public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
    }
}

package org.example.publishers;

import org.example.enums.PaymentEventType;
import org.example.messages.PaymentEventMessage;
import org.example.models.Payment;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    public void publishPaymentEvent(PaymentEventType eventType, Payment payment) {
        PaymentEventMessage message = new PaymentEventMessage(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                eventType
        );

        rabbitTemplate.convertAndSend(paymentsExchangeName, paymentsRoutingKey+".event", message);
    }


}

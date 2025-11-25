package org.example.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentsEventsListener { //sera para fazer aqui ou nas orders??

    @RabbitListener(queues = "${app.payments.queue}")
    public void handlePaymentEvent(PaymentEventsPublisher.PaymentEventMessage message) {
        // Aqui atualizas o estado da encomenda, etc.
        // Ex.: se eventType = "CAPTURE", marcar Order como PAYED.
    }
}

package org.example.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Mensagem publicada no broker.
 *
 * Nota: NÃO usar Serializable para microserviços.
 * Com Jackson2JsonMessageConverter, isto vai em JSON.
 */
@Getter
@Setter
@AllArgsConstructor
public class PaymentEventMessage {

    private UUID paymentId;
    private UUID orderId;
    private UUID userId;
    private float amount;
    private String status;
    private String eventType;

}
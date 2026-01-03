package org.example.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.enums.PaymentEventType;
import org.example.enums.PaymentStatus;

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
@NoArgsConstructor
public class PaymentEventMessage {

    private UUID paymentId;
    private UUID orderId;
    private UUID userId;
    private float amount;
    private PaymentStatus status;
    private PaymentEventType eventType;

}
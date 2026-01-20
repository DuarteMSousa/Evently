package org.evently.orders.messages.received;

import lombok.*;
import org.evently.orders.enums.externalServices.payments.PaymentEventType;
import org.evently.orders.enums.externalServices.payments.PaymentStatus;

import java.util.UUID;

@Data
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
package org.example.messages.externalServices;

import lombok.*;
import org.example.enums.externalServices.PaymentEventType;
import org.example.enums.externalServices.PaymentStatus;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventMessage {
    private UUID paymentId;
    private UUID orderId;
    private UUID userId;
    private float amount;
    private PaymentStatus status;
    private PaymentEventType eventType;
}
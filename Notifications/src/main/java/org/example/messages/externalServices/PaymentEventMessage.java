package org.example.messages.externalServices;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.enums.externalServices.PaymentEventType;
import org.example.enums.externalServices.PaymentStatus;

import java.util.UUID;

@NoArgsConstructor
@Getter @Setter
public class PaymentEventMessage {
    private UUID paymentId;
    private UUID orderId;
    private UUID userId;
    private float amount;
    private PaymentStatus status;
    private PaymentEventType eventType;
}
package org.example.messages.externalServices;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter @Setter
public class PaymentEventMessage {
    private UUID paymentId;
    private UUID orderId;
    private UUID userId;
    private float amount;
    private String status;     // String, não enum
    private String eventType;  // String, não enum
}
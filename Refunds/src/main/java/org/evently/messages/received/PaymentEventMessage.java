package org.evently.messages.received;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.evently.enums.externalServices.PaymentEventType;
import org.evently.enums.externalServices.PaymentStatus;

import java.util.UUID;

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
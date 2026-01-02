package org.evently.messages.externalServices;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.evently.enums.externalServices.PaymentEventType;
import org.evently.enums.externalServices.PaymentStatus;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class PaymentEventMessage {

    private UUID paymentId;
    private UUID orderId;
    private UUID userId;
    private float amount;
    private PaymentStatus status;
    private PaymentEventType eventType;

}
package org.example.messages.externalServices;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class RefundEventMessage {

    private UUID refundId;
    private UUID paymentId;
    private float amount;
    private String status;
    private String eventType;
}

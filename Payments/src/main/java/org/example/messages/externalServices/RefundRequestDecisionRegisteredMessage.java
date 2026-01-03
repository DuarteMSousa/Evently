package org.example.messages.externalServices;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.enums.externalServices.DecisionType;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class RefundRequestDecisionRegisteredMessage {

    private UUID userToRefundId;
    private UUID paymentId;
    private DecisionType decisionType;
    private String description;
}

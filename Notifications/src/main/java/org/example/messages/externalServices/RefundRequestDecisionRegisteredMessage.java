package org.example.messages.externalServices;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.externalServices.DecisionType;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RefundRequestDecisionRegisteredMessage  {

    private UUID userToRefundId;
    private UUID paymentId;
    private DecisionType decisionType;
    private String description;
}
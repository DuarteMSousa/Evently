package org.evently.orders.messages.received;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.evently.orders.enums.externalServices.refunds.DecisionType;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RefundRequestDecisionRegisteredMessage {

    private UUID userToRefundId;

    private UUID paymentId;

    private UUID orderId;

    private DecisionType decisionType;

    private String description;

}

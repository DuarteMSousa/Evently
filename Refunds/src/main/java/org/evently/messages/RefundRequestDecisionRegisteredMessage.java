package org.evently.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.evently.enums.DecisionType;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RefundRequestDecisionRegisteredMessage {

    private UUID refundRequestId;

    private DecisionType decisionType;

    private String description;

}

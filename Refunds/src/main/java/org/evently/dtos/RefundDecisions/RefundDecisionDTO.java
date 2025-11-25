package org.evently.dtos.RefundDecisions;

import lombok.Getter;
import lombok.Setter;
import org.evently.enums.DecisionType;

import java.util.UUID;

@Setter
@Getter
public class RefundDecisionDTO {

    private UUID id;

    private UUID decidedBy;

    private DecisionType decisionType;

    private String description;

    private UUID refundRequestId;

}

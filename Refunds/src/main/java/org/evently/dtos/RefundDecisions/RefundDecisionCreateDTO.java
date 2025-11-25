package org.evently.dtos.RefundDecisions;

import lombok.Getter;
import lombok.Setter;
import org.evently.enums.DecisionType;

import java.util.Date;
import java.util.UUID;

@Setter
@Getter
public class RefundDecisionCreateDTO {

    private UUID refundRequestId;

    private UUID decidedBy;

    private DecisionType decisionType;

    private String description;

}

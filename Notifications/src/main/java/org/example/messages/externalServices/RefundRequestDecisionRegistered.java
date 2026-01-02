package org.example.messages.externalServices;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RefundRequestDecisionRegistered {

    private UUID id;
    private UUID decidedBy;
    private String decisionType;
    private String description;
    private UUID refundRequestId;
}
package org.evently.dtos.RefundRequests;

import lombok.Getter;
import lombok.Setter;
import org.evently.enums.RefundRequestStatus;

import java.util.Date;
import java.util.UUID;

@Setter
@Getter
public class RefundRequestDTO {

    private UUID id;

    private UUID paymentId;

    private UUID userId;

    private UUID orderId;

    private String title;

    private String description;

    private RefundRequestStatus status;

    private Date createdAt;

    private Date decisionAt;

    private Date processedAt;

}

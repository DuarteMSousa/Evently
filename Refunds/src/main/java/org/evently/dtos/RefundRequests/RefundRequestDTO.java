package org.evently.dtos.RefundRequests;

import org.evently.enums.RefundRequestStatus;

import java.util.UUID;

public class RefundRequestDTO {

    private UUID id;

    private UUID paymentId;

    private UUID userId;

    private String title;

    private String description;

    private RefundRequestStatus status;

}

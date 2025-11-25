package org.evently.dtos.RefundRequests;

import lombok.Getter;
import lombok.Setter;
import org.evently.enums.RefundRequestStatus;

import java.util.UUID;

@Setter
@Getter
public class RefundRequestCreateDTO {

    private UUID payment;

    private UUID user;

    private String title;

    private String description;

    private RefundRequestStatus status;

}

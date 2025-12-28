package org.evently.dtos.externalServices;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PaymentStatusDTO {

    private UUID paymentId;
    private String status;
}
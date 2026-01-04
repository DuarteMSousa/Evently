package org.evently.dtos.externalServices;

import lombok.Getter;
import lombok.Setter;
import org.evently.enums.externalServices.PaymentStatus;

import java.util.UUID;

@Getter
@Setter
public class PaymentStatusDTO {

    private UUID paymentId;
    private PaymentStatus status;
}
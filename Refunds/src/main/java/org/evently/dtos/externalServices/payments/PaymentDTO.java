package org.evently.dtos.externalServices.payments;

import lombok.Getter;
import lombok.Setter;
import org.evently.enums.externalServices.payments.PaymentProvider;
import org.evently.enums.externalServices.payments.PaymentStatus;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class PaymentDTO {

    private UUID id;
    private UUID orderId;
    private UUID userId;
    private float amount;
    private PaymentStatus status;
    private PaymentProvider provider;
    private String providerRef;
    private Date createdAt;
    private Date updatedAt;
}
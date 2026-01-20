package org.example.dtos;

import lombok.Getter;
import lombok.Setter;
import org.example.enums.PaymentProvider;
import org.example.enums.PaymentStatus;

import java.util.UUID;

@Getter
@Setter
public class PaymentCreatedResponseDTO {

    private UUID paymentId;

    private PaymentStatus status;

    private PaymentProvider provider;

    private String providerRef;

    private String approvalUrl;

}
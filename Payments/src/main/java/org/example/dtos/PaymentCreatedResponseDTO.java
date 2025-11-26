package org.example.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PaymentCreatedResponseDTO {
    private UUID paymentId;
    private String status;
    private String provider;
    private String providerRef;
    private String approvalUrl;
}
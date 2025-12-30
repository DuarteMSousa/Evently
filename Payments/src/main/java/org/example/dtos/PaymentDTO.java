package org.example.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class PaymentDTO {

    private UUID id;
    private UUID orderId;
    private UUID userId;
    private float amount;
    private String status;
    private String provider;
    private String providerRef;
    private Date createdAt;
    private Date updatedAt;
}
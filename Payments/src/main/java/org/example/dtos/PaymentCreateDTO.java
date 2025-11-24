package org.example.dtos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class PaymentCreateDTO {

    private UUID orderId;
    private UUID userId;
    private BigDecimal amount;
    private String provider;
}
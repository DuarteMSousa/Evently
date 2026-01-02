package org.example.dtos;

import lombok.Getter;
import lombok.Setter;
import org.example.enums.PaymentProvider;

import java.util.UUID;

@Getter
@Setter
public class PaymentCreateDTO {

    private UUID orderId;
    private UUID userId;
    private float amount;
    private PaymentProvider provider;
}
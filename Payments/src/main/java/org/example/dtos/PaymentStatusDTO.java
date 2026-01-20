package org.example.dtos;

import lombok.Getter;
import lombok.Setter;
import org.example.enums.PaymentStatus;

import java.util.UUID;

@Getter
@Setter
public class PaymentStatusDTO {

    private UUID paymentId;

    private PaymentStatus status;

}
package org.evently.dtos.externalServices;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OrderLineDTO {

    private UUID orderId;

    private UUID productId;

    private Integer quantity;

    private float unitPrice;

}

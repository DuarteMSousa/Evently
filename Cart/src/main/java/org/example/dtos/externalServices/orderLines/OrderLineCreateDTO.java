package org.example.dtos.externalServices.orderLines;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OrderLineCreateDTO {

    private UUID productId;

    private Integer quantity;

}

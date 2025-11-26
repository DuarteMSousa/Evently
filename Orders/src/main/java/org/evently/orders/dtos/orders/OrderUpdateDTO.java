package org.evently.orders.dtos.orders;

import lombok.Getter;
import lombok.Setter;
import org.evently.orders.enums.OrderStatus;

import java.util.UUID;

@Getter
@Setter
public class OrderUpdateDTO {

    private UUID id;

    private OrderStatus status;

}

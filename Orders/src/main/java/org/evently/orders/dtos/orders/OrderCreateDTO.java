package org.evently.orders.dtos.orders;

import lombok.Getter;
import lombok.Setter;
import org.evently.orders.dtos.orderLines.OrderLineCreateDTO;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class OrderCreateDTO {

    private UUID userId;

    private List<OrderLineCreateDTO> lines;

}

package org.evently.orders.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.evently.orders.dtos.orderLines.OrderLineDTO;
import org.evently.orders.enums.OrderStatus;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderPaidMessage {

    private UUID id;

    private UUID userId;

    private OrderStatus status;

    private float total;

    private List<OrderLineDTO> lines;

}

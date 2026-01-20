package org.example.messages.received;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.dtos.externalServices.orderLines.OrderLineDTO;
import org.example.enums.externalServices.orders.OrderStatus;


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

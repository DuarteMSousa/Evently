package org.example.messages.received;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.dtos.externalServices.orderLines.OrderLineDTO;
import org.example.enums.externalServices.OrderStatus;


import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderPayedMessage {

    private UUID id;

    private UUID userId;

    private OrderStatus status;

    private float total;

    private List<OrderLineDTO> lines;

}

package org.example.dtos.externalServices;

import lombok.Getter;
import lombok.Setter;
import org.example.enums.externalServices.OrderStatus;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class OrderDTO {

    private UUID id;

    private UUID userId;

    private OrderStatus status;

    private float total;

    private Date createdAt;

    private Date paidAt;

    private Date canceledAt;

    private List<OrderLineDTO> lines;

}

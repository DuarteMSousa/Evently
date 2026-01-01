package org.example.dtos.externalServices.orders;

import lombok.Getter;
import lombok.Setter;
import org.example.dtos.externalServices.orderLines.OrderLineCreateDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class OrderCreateDTO {

    private UUID userId;

    private List<OrderLineCreateDTO> lines = new ArrayList<>();;

}

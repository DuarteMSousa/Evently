package org.example.dtos.externalServices.ticketStocks;

import lombok.Data;

@Data
public class TicketStockCreateDTO {

    private TicketStockIdDTO id;

    private Integer availableQuantity;
}

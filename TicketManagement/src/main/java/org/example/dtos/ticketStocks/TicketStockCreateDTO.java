package org.example.dtos.ticketStocks;

import lombok.Data;
import org.example.models.TicketStockId;

@Data
public class TicketStockCreateDTO {
    
    private TicketStockId id;

    private Integer availableQuantity;
}

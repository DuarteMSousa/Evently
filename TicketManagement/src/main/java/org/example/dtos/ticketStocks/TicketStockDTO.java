package org.example.dtos.ticketStocks;

import lombok.Data;
import org.example.models.TicketStockId;

import java.util.Date;

@Data
public class TicketStockDTO {

    private TicketStockId id;

    private Integer availableQuantity;

    private Date createdAt;

    private Date updatedAt;
}

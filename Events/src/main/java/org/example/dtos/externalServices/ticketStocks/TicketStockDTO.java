package org.example.dtos.externalServices.ticketStocks;

import lombok.Data;

import java.util.Date;

@Data
public class TicketStockDTO {

    private TicketStockIdDTO id;

    private Integer availableQuantity;

    private Date createdAt;

    private Date updatedAt;
}

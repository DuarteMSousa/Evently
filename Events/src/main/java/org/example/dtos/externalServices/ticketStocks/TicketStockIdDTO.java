package org.example.dtos.externalServices.ticketStocks;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketStockIdDTO implements Serializable {

    private UUID eventId;

    private UUID sessionId;

    private UUID tierId;

}

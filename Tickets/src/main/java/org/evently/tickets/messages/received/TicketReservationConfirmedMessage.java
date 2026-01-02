package org.evently.tickets.messages.received;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TicketReservationConfirmedMessage {

    private UUID id;

    private UUID userId;

    private UUID orderId;

    private UUID tierId;

    private UUID sessionId;

    private UUID eventId;

    private Integer quantity;

}

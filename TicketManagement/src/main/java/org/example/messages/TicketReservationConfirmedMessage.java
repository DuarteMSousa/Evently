package org.example.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class TicketReservationConfirmedMessage {

    private UUID id;

    private UUID userId;

    private UUID orderId;

    private UUID tierId;

    private UUID sessionId;

    private UUID eventId;

    private Integer quantity;

}

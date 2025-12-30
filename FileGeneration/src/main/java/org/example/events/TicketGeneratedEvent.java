package org.example.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TicketGeneratedEvent {
    private UUID id;

    private UUID reservationId;

    private UUID orderId;

    private UUID userId;

    private UUID eventId;

    private UUID sessionId;

    private UUID tierId;
}

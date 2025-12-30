package org.example.events;

import jakarta.persistence.Column;

import java.util.UUID;

public class TicketReservationConfirmedEvent {

    private UUID id;

    private UUID userId;

    private UUID orderId;

    private UUID tierId;

    private UUID sessionId;

    private UUID eventId;

    private Integer quantity;

}

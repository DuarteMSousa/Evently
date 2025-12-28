package org.example.dtos;

import java.util.UUID;

public class TicketReservationCreateDTO {

    private UUID userId;

    private UUID orderId;

    private UUID tierId;

    private UUID sessionId;

    private UUID eventId;

    private Integer quantity;
}

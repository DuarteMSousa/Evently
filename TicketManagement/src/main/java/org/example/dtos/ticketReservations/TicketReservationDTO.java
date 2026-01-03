package org.example.dtos.ticketReservations;

import org.example.enums.TicketReservationStatus;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;


public class TicketReservationDTO {

    private UUID id;

    private UUID userId;

    private UUID orderId;

    private UUID tierId;

    private UUID sessionId;

    private UUID eventId;

    private Integer quantity;

    private TicketReservationStatus status;

    private OffsetDateTime expiresAt;

    private OffsetDateTime releasedAt;

    private OffsetDateTime confirmedAt;

    private Date createdAt;
}

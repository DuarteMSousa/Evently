package org.evently.tickets.dtos;

import lombok.Getter;
import lombok.Setter;
import org.evently.tickets.enums.TicketStatus;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class TicketDTO {

    private UUID id;

    private UUID reservationId;

    private UUID orderId;

    private UUID userId;

    private UUID eventId;

    private UUID sessionId;

    private UUID tierId;

    private TicketStatus status;

    private Date issuedAt;

    private Date validatedAt;

}

package org.evently.orders.dtos.externalServices;

import lombok.Getter;
import lombok.Setter;
import org.evently.orders.enums.externalServices.TicketReservationStatus;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
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

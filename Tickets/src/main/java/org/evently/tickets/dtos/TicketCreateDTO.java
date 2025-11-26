package org.evently.tickets.dtos;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class TicketCreateDTO {

    private UUID reservationId;

    private UUID orderId;

    private UUID userId;

    private UUID eventId;

    private UUID sessionId;

    private UUID tierId;

}

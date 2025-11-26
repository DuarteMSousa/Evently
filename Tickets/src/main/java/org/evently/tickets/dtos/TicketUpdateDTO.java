package org.evently.tickets.dtos;

import lombok.Getter;
import lombok.Setter;
import org.evently.tickets.enums.TicketStatus;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class TicketUpdateDTO {

    private UUID id;

    private TicketStatus status;

    private Date validatedAt;

}

package org.example.messages.received;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Data
@NoArgsConstructor
@Setter
public class EventTicketStockGeneratedMessage {

    UUID eventId;

}

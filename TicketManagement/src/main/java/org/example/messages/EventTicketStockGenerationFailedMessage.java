package org.example.messages;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Data
@NoArgsConstructor
@Setter
public class EventTicketStockGenerationFailedMessage {

    UUID eventId;

}

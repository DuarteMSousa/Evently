package org.example.messages.received;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderCreatedMessage {

    private UUID id;

    private UUID userId;

    private float total;

}

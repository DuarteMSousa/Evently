package org.evently.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RefundRequestMessageSent {

    private UUID id;

    private UUID userId;

    private String content;

    private UUID refundRequestId;

}

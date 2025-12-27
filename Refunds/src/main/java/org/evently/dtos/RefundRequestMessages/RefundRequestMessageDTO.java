package org.evently.dtos.RefundRequestMessages;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Setter
@Getter
public class RefundRequestMessageDTO {

    private UUID id;

    private UUID userId;

    private String content;

    private UUID refundRequestId;

    private Date createdAt;
}

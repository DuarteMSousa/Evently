package org.evently.dtos.RefundRequestMessages;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class RefundRequestMessageCreateDTO {

    private UUID user;

    private UUID refundRequestId;

    private String content;
}

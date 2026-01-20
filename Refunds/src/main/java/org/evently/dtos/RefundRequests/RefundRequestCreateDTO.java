package org.evently.dtos.RefundRequests;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class RefundRequestCreateDTO {

    private UUID order;

    private String title;

    private String description;

}

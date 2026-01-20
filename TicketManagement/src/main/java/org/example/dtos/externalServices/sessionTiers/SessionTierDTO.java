package org.example.dtos.externalServices.sessionTiers;

import lombok.Getter;

import java.util.Date;
import java.util.UUID;

@Getter
public class SessionTierDTO {

    private UUID id;

    private UUID eventSessionId;

    private UUID zoneId;

    private float price;

    private UUID createdBy;

    private Date createdAt;

    private UUID updatedBy;

    private Date updatedAt;

}

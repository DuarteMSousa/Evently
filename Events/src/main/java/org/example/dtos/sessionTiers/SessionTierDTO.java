package org.example.dtos.sessionTiers;


import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class SessionTierDTO {

    private UUID id;

    private UUID eventSessionId;

    private UUID zoneId;

    private BigDecimal price;

    private UUID createdBy;

    private Date createdAt;

    private UUID updatedBy;

    private Date updatedAt;
}

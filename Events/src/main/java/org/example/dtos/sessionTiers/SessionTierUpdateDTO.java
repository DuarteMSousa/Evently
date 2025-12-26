package org.example.dtos.sessionTiers;


import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class SessionTierUpdateDTO {

    private UUID id;

    private UUID eventSessionId;

    private UUID zoneId;

    private BigDecimal price;
}

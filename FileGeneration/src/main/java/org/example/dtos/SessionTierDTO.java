package org.example.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SessionTierDTO {

    private UUID id;

    private UUID zoneId;

    private float price;

    private Date createdAt;

    private UUID updatedBy;

    private Date updatedAt;
}

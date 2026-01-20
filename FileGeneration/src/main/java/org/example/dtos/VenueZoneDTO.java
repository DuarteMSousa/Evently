package org.example.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class VenueZoneDTO {

    private UUID id;

    private UUID venueId;

    private String venueName;

    private String name;

    private Integer capacity;

    private UUID createdBy;
    private Date createdAt;
    private UUID updatedBy;
    private Date updatedAt;
}
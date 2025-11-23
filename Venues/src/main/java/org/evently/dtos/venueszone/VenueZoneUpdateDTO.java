package org.evently.dtos.venueszone;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VenueZoneUpdateDTO {

    private UUID id;
    private String name;
    private Integer capacity;
    private UUID updatedBy;
}
package org.evently.dtos.venueszone;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VenueZoneCreateDTO {

    private UUID venueId;

    private String name;

    private Integer capacity;

    private UUID createdBy;

}
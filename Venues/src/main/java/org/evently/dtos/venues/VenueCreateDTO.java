package org.evently.dtos.venues;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VenueCreateDTO {

    private String name;
    private Integer capacity;
    private String address;
    private String city;
    private String country;
    private String postalCode;

    private UUID createdBy;
}
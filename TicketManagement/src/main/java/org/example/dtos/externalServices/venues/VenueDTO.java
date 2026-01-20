package org.example.dtos.externalServices.venues;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class VenueDTO {

    private UUID id;

    private String name;

    private Integer capacity;

    private String address;

    private String city;

    private String country;

    private String postalCode;

    private boolean active;

    private UUID createdBy;
    private Date createdAt;
    private UUID updatedBy;
    private Date updatedAt;
}
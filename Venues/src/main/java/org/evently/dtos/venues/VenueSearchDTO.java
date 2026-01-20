package org.evently.dtos.venues;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VenueSearchDTO {

    private String name;

    private String city;

    private String country;

    private Integer minCapacity;

    private Boolean onlyActive;

}

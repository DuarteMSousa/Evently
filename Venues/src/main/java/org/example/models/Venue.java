package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Venue {

    private UUID id;
    private String name;
    private int capacity;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    private boolean active;

    private UUID createdBy;
    private Instant createdAt;
    private UUID updatedBy;
    private Instant updatedAt;

    private List<VenueZone> zones;
}

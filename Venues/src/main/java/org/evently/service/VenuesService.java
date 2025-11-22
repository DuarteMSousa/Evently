package org.evently.service;

import org.evently.models.Venue;
import org.evently.models.VenueZone;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class VenuesService {

    // "Base de dados" em memória
    private final Map<UUID, Venue> venues = new ConcurrentHashMap<>();

    // GET /get-venue/{venueId}
    public Venue getVenue(UUID id) {
        return venues.get(id);
    }

    // POST /search-venues
    public List<Venue> searchVenues(String name, String city, String country, Boolean active) {
        return venues.values().stream()
                .filter(v -> name == null ||
                        v.getName().toLowerCase().contains(name.toLowerCase()))
                .filter(v -> city == null ||
                        v.getCity().equalsIgnoreCase(city))
                .filter(v -> country == null ||
                        v.getCountry().equalsIgnoreCase(country))
                .filter(v -> active == null ||
                        v.isActive() == active)
                .collect(Collectors.toList());
    }

    // POST /create-venue
    public UUID createVenue(Venue venue) {

        // validações básicas
        if (venue.getCapacity() <= 0) {
            throw new IllegalArgumentException("Venue capacity must be > 0");
        }

        int totalZonesCapacity = 0;
        if (venue.getZones() != null) {
            totalZonesCapacity = venue.getZones().stream()
                    .mapToInt(VenueZone::getCapacity)
                    .sum();
        }

        if (totalZonesCapacity > venue.getCapacity()) {
            throw new IllegalArgumentException(
                    "Total zones capacity cannot be greater than venue capacity");
        }

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        venue.setId(id);
        venue.setActive(true);
        venue.setCreatedAt(now);
        venue.setUpdatedAt(null);
        venue.setUpdatedBy(null);

        if (venue.getZones() != null) {
            for (VenueZone zone : venue.getZones()) {
                if (zone.getId() == null) {
                    zone.setId(UUID.randomUUID());
                }
                zone.setVenueId(id);
                zone.setCreatedAt(now);
                zone.setCreatedBy(venue.getCreatedBy());
            }
        }

        venues.put(id, venue);
        return id;
    }

    // PUT /deactivate-venue/{id}
    public boolean deactivateVenue(UUID id) {
        Venue venue = venues.get(id);
        if (venue == null) {
            return false;
        }

        if (!venue.isActive()) {
            return true;
        }

        venue.setActive(false);
        venue.setUpdatedAt(Instant.now());

        return true;
    }
}

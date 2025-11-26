package org.evently.service;

import org.evently.exceptions.InvalidVenueZoneException;
import org.evently.exceptions.VenueNotFoundException;
import org.evently.exceptions.VenueZoneNotFoundException;
import org.evently.models.Venue;
import org.evently.models.VenueZone;
import org.evently.repositories.VenueZoneRepository;
import org.evently.repositories.VenuesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class VenueZonesService {

    @Autowired
    private VenueZoneRepository venueZoneRepository;

    @Autowired
    private VenuesRepository venuesRepository;

    private void validateZone(Venue venue, VenueZone zone) {
        if (zone.getName() == null) {
            throw new InvalidVenueZoneException("Zone name is required");
        }
        if (zone.getCapacity() == null || zone.getCapacity() <= 0) {
            throw new InvalidVenueZoneException("Zone capacity must be greater than 0");
        }
        if (zone.getCapacity() > venue.getCapacity()) {
            throw new InvalidVenueZoneException("Zone capacity must be less or equal than venue capacity");
        }
        if (zone.getCreatedBy() == null && zone.getId() == null) {
            throw new InvalidVenueZoneException("CreatedBy is required");
        }
    }

    @Transactional
    public VenueZone createVenueZone(UUID venueId, VenueZone zone) {
        Venue venue = venuesRepository.findById(venueId)
                .orElseThrow(() -> new VenueNotFoundException("Venue not found"));

        validateZone(venue, zone);

        zone.setVenue(venue);
        return venueZoneRepository.save(zone);
    }

    public VenueZone getVenueZone(UUID zoneId) {
        return venueZoneRepository.findById(zoneId)
                .orElseThrow(() -> new VenueZoneNotFoundException("Venue zone not found"));
    }

    public List<VenueZone> getVenueZonesByVenue(UUID venueId) {
        if (!venuesRepository.existsById(venueId)) {
            throw new VenueNotFoundException("Venue not found");
        }
        return venueZoneRepository.findByVenueId(venueId);
    }

    @Transactional
    public VenueZone updateVenueZone(UUID zoneId, VenueZone zone) {
        if (zone.getId() != null && !zone.getId().equals(zoneId)) {
            throw new InvalidVenueZoneException("Parameter id and body id do not correspond");
        }

        VenueZone existing = venueZoneRepository.findById(zoneId)
                .orElseThrow(() -> new VenueZoneNotFoundException("Venue zone not found"));

        if (zone.getName() != null) {
            existing.setName(zone.getName());
        }
        if (zone.getCapacity() != null) {
            validateZone(existing.getVenue(), zone); // valida nova capacidade
            existing.setCapacity(zone.getCapacity());
        }
        if (zone.getUpdatedBy() == null) {
            throw new InvalidVenueZoneException("UpdatedBy is required");
        }
        existing.setUpdatedBy(zone.getUpdatedBy());

        return venueZoneRepository.save(existing);
    }
}
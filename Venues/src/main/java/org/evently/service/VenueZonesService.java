package org.evently.service;

import org.evently.exceptions.InvalidVenueZoneException;
import org.evently.exceptions.VenueNotFoundException;
import org.evently.exceptions.VenueZoneNotFoundException;
import org.evently.models.Venue;
import org.evently.models.VenueZone;
import org.evently.repositories.VenueZoneRepository;
import org.evently.repositories.VenuesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class VenueZonesService {

    private static final Logger logger = LoggerFactory.getLogger(VenueZonesService.class);

    private static final Marker ZONE_CREATE = MarkerFactory.getMarker("VENUE_ZONE_CREATE");
    private static final Marker ZONE_GET = MarkerFactory.getMarker("VENUE_ZONE_GET");
    private static final Marker ZONE_LIST = MarkerFactory.getMarker("VENUE_ZONE_LIST");
    private static final Marker ZONE_UPDATE = MarkerFactory.getMarker("VENUE_ZONE_UPDATE");
    private static final Marker ZONE_VALIDATION = MarkerFactory.getMarker("VENUE_ZONE_VALIDATION");

    @Autowired
    private VenueZoneRepository venueZoneRepository;

    @Autowired
    private VenuesRepository venuesRepository;

    private void validateZone(Venue venue, VenueZone zone) {
        logger.debug(ZONE_VALIDATION,
                "Validating venue zone (venueId={}, zoneId={}, name={}, capacity={})",
                venue != null ? venue.getId() : null,
                zone.getId(),
                zone.getName(),
                zone.getCapacity()
        );

        if (zone.getName() == null) {
            logger.warn(ZONE_VALIDATION, "Missing zone name (venueId={}, zoneId={})",
                    venue != null ? venue.getId() : null, zone.getId());
            throw new InvalidVenueZoneException("Zone name is required");
        }
        if (zone.getCapacity() == null || zone.getCapacity() <= 0) {
            logger.warn(ZONE_VALIDATION, "Invalid zone capacity={} (venueId={}, zoneId={})",
                    zone.getCapacity(), venue != null ? venue.getId() : null, zone.getId());
            throw new InvalidVenueZoneException("Zone capacity must be greater than 0");
        }
        if (venue != null && venue.getCapacity() != null && zone.getCapacity() > venue.getCapacity()) {
            logger.warn(ZONE_VALIDATION, "Zone capacity {} exceeds venue capacity {} (venueId={}, zoneId={})",
                    zone.getCapacity(), venue.getCapacity(), venue.getId(), zone.getId());
            throw new InvalidVenueZoneException("Zone capacity must be less or equal than venue capacity");
        }
        if (zone.getCreatedBy() == null && zone.getId() == null) {
            logger.warn(ZONE_VALIDATION, "Missing createdBy for new zone (venueId={})",
                    venue != null ? venue.getId() : null);
            throw new InvalidVenueZoneException("CreatedBy is required");
        }
    }

    @Transactional
    public VenueZone createVenueZone(UUID venueId, VenueZone zone) {
        logger.info(ZONE_CREATE, "Create venue zone requested (venueId={}, name={}, capacity={})",
                venueId, zone.getName(), zone.getCapacity());

        Venue venue = venuesRepository.findById(venueId)
                .orElseThrow(() -> {
                    logger.warn(ZONE_CREATE, "Venue not found for zone creation (venueId={})", venueId);
                    return new VenueNotFoundException("Venue not found");
                });

        validateZone(venue, zone);

        zone.setVenue(venue);
        VenueZone saved = venueZoneRepository.save(zone);

        logger.info(ZONE_CREATE, "Venue zone created successfully (venueId={}, zoneId={}, name={})",
                venueId, saved.getId(), saved.getName());

        return saved;
    }

    public VenueZone getVenueZone(UUID zoneId) {
        logger.debug(ZONE_GET, "Get venue zone requested (zoneId={})", zoneId);

        return venueZoneRepository.findById(zoneId)
                .orElseThrow(() -> {
                    logger.warn(ZONE_GET, "Venue zone not found (zoneId={})", zoneId);
                    return new VenueZoneNotFoundException("Venue zone not found");
                });
    }

    public List<VenueZone> getVenueZonesByVenue(UUID venueId) {
        logger.debug(ZONE_LIST, "List venue zones requested (venueId={})", venueId);

        if (!venuesRepository.existsById(venueId)) {
            logger.warn(ZONE_LIST, "Venue not found for zone listing (venueId={})", venueId);
            throw new VenueNotFoundException("Venue not found");
        }

        List<VenueZone> zones = venueZoneRepository.findByVenueId(venueId);

        logger.debug(ZONE_LIST, "List venue zones completed (venueId={}, results={})",
                venueId, zones.size());

        return zones;
    }

    @Transactional
    public VenueZone updateVenueZone(UUID zoneId, VenueZone zone) {
        logger.info(ZONE_UPDATE, "Update venue zone requested (zoneId={}, bodyId={}, name={}, capacity={}, updatedBy={})",
                zoneId, zone.getId(), zone.getName(), zone.getCapacity(), zone.getUpdatedBy());

        if (zone.getId() != null && !zone.getId().equals(zoneId)) {
            logger.warn(ZONE_UPDATE, "Mismatch between path id and body id (pathId={}, bodyId={})",
                    zoneId, zone.getId());
            throw new InvalidVenueZoneException("Parameter id and body id do not correspond");
        }

        VenueZone existing = venueZoneRepository.findById(zoneId)
                .orElseThrow(() -> {
                    logger.warn(ZONE_UPDATE, "Venue zone not found for update (zoneId={})", zoneId);
                    return new VenueZoneNotFoundException("Venue zone not found");
                });

        if (zone.getName() != null) {
            existing.setName(zone.getName());
        }

        if (zone.getCapacity() != null) {
            VenueZone toValidate = new VenueZone();
            toValidate.setId(existing.getId());
            toValidate.setName(zone.getName() != null ? zone.getName() : existing.getName());
            toValidate.setCapacity(zone.getCapacity());
            toValidate.setCreatedBy(existing.getCreatedBy());

            validateZone(existing.getVenue(), toValidate);

            existing.setCapacity(zone.getCapacity());
        }

        if (zone.getUpdatedBy() == null) {
            logger.warn(ZONE_UPDATE, "Missing updatedBy (zoneId={})", zoneId);
            throw new InvalidVenueZoneException("UpdatedBy is required");
        }

        existing.setUpdatedBy(zone.getUpdatedBy());

        VenueZone saved = venueZoneRepository.save(existing);

        logger.info(ZONE_UPDATE, "Venue zone updated successfully (zoneId={}, name={}, capacity={})",
                saved.getId(), saved.getName(), saved.getCapacity());

        return saved;
    }
}

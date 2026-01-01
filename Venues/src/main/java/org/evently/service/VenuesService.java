package org.evently.service;

import org.evently.dtos.venues.VenueSearchDTO;
import org.evently.exceptions.InvalidVenueException;
import org.evently.exceptions.VenueAlreadyDeactivatedException;
import org.evently.exceptions.VenueNotFoundException;
import org.evently.models.Venue;
import org.evently.repositories.VenuesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class VenuesService {

    private static final Logger logger = LoggerFactory.getLogger(VenuesService.class);

    private static final Marker VENUE_CREATE = MarkerFactory.getMarker("VENUE_CREATE");
    private static final Marker VENUE_DEACTIVATE = MarkerFactory.getMarker("VENUE_DEACTIVATE");
    private static final Marker VENUE_GET = MarkerFactory.getMarker("VENUE_GET");
    private static final Marker VENUE_SEARCH = MarkerFactory.getMarker("VENUE_SEARCH");
    private static final Marker VENUE_VALIDATION = MarkerFactory.getMarker("VENUE_VALIDATION");


    @Autowired
    private VenuesRepository venuesRepository;

    /**
     * Validates all required fields of a venue before creation (and potentially before updates).
     *
     *
     * @param venue venue to validate
     * @throws InvalidVenueException if any required field is missing or invalid
     */
    private void validateVenue(Venue venue) {
        logger.debug(VENUE_VALIDATION, "Validating venue payload (id={})", venue.getId());

        if (venue.getCapacity() == null || venue.getCapacity() <= 0) {
            logger.warn(VENUE_VALIDATION, "Invalid capacity: {}", venue.getCapacity());
            throw new InvalidVenueException("Capacity must be greater than 0");
        }
        if (venue.getName() == null) {
            logger.warn(VENUE_VALIDATION, "Missing name");
            throw new InvalidVenueException("Name is required");
        }
        if (venue.getAddress() == null) {
            logger.warn(VENUE_VALIDATION, "Missing address");
            throw new InvalidVenueException("Address is required");
        }
        if (venue.getCity() == null) {
            logger.warn(VENUE_VALIDATION, "Missing city");
            throw new InvalidVenueException("City is required");
        }
        if (venue.getCountry() == null) {
            logger.warn(VENUE_VALIDATION, "Missing country");
            throw new InvalidVenueException("Country is required");
        }
        if (venue.getPostalCode() == null) {
            logger.warn(VENUE_VALIDATION, "Missing postal code");
            throw new InvalidVenueException("Postal code is required");
        }
        if (venue.getCreatedBy() == null && venue.getId() == null) {
            logger.warn(VENUE_VALIDATION, "Missing createdBy for new venue");
            throw new InvalidVenueException("CreatedBy is required");
        }
    }

    /**
     * Creates a new venue after validating its payload.
     *
     *
     * @param venue venue to create
     * @return persisted venue
     * @throws InvalidVenueException if payload is invalid or the venue name already exists
     */
    @Transactional
    public Venue createVenue(Venue venue) {
        logger.info(VENUE_CREATE, "Create venue requested (name={}, city={}, country={})",
                venue.getName(), venue.getCity(), venue.getCountry());

        validateVenue(venue);

        if (venuesRepository.existsByName(venue.getName())) {
            logger.warn(VENUE_CREATE, "Venue name already exists: {}", venue.getName());
            throw new InvalidVenueException("Venue with name " + venue.getName() + " already exists");
        }

        venue.setActive(true);
        Venue saved = venuesRepository.save(venue);

        logger.info(VENUE_CREATE, "Venue created successfully (id={}, name={})",
                saved.getId(), saved.getName());

        return saved;
    }

    /**
     * Deactivates an existing venue (soft-deactivate).
     *
     * @param id venue identifier
     * @return updated venue with active=false
     * @throws VenueNotFoundException if the venue does not exist
     * @throws VenueAlreadyDeactivatedException if the venue is already deactivated
     */
    @Transactional
    public Venue deactivateVenue(UUID id) {
        logger.info(VENUE_DEACTIVATE, "Deactivate venue requested (id={})", id);

        Venue venue = venuesRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(VENUE_DEACTIVATE, "Venue not found (id={})", id);
                    return new VenueNotFoundException("Venue not found");
                });

        if (!venue.isActive()) {
            logger.warn(VENUE_DEACTIVATE, "Venue already deactivated (id={})", id);
            throw new VenueAlreadyDeactivatedException("Venue already deactivated");
        }

        venue.setActive(false);
        Venue saved = venuesRepository.save(venue);

        logger.info(VENUE_DEACTIVATE, "Venue deactivated successfully (id={})", saved.getId());

        return saved;
    }

    /**
     * Retrieves a venue by its unique identifier.
     *
     * @param id venue identifier
     * @return found venue
     * @throws VenueNotFoundException if the venue does not exist
     */
    public Venue getVenue(UUID id) {
        logger.debug(VENUE_GET, "Get venue requested (id={})", id);

        return venuesRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(VENUE_GET, "Venue not found (id={})", id);
                    return new VenueNotFoundException("Venue not found");
                });
    }

    /**
     * Searches venues based on dynamic criteria using JPA Specifications.
     *
     *
     * @param criteria search criteria
     * @return list of venues matching the criteria
     * @throws InvalidVenueException if criteria contains invalid fields (e.g., minCapacity < 0)
     */
    public List<Venue> searchVenues(VenueSearchDTO criteria) {
        logger.debug(VENUE_SEARCH,
                "Search venues requested (onlyActive={}, name={}, city={}, country={}, minCapacity={})",
                criteria.getOnlyActive(),
                criteria.getName(),
                criteria.getCity(),
                criteria.getCountry(),
                criteria.getMinCapacity()
        );

        if (criteria.getMinCapacity() != null && criteria.getMinCapacity() < 0) {
            logger.warn(VENUE_SEARCH, "Invalid minCapacity: {}", criteria.getMinCapacity());
            throw new InvalidVenueException("minCapacity must be >= 0");
        }

        Specification<Venue> spec = Specification.where(null);

        if (criteria.getOnlyActive() != null && criteria.getOnlyActive()) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("active")));
        }

        if (criteria.getName() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + criteria.getName().toLowerCase() + "%"));
        }

        if (criteria.getCity() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("city")), criteria.getCity().toLowerCase()));
        }

        if (criteria.getCountry() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("country")), criteria.getCountry().toLowerCase()));
        }

        if (criteria.getMinCapacity() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("capacity"), criteria.getMinCapacity()));
        }

        List<Venue> results = venuesRepository.findAll(spec);

        logger.debug(VENUE_SEARCH, "Search venues completed (results={})", results.size());
        return results;
    }
}
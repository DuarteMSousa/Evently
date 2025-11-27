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

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VenuesService {

    @Autowired
    private VenuesRepository venuesRepository;

    private void validateVenue(Venue venue) {
        if (venue.getCapacity() == null || venue.getCapacity() <= 0) {
            throw new InvalidVenueException("Capacity must be greater than 0");
        }
        if (venue.getName() == null) {
            throw new InvalidVenueException("Name is required");
        }
        if (venue.getAddress() == null ) {
            throw new InvalidVenueException("Address is required");
        }
        if (venue.getCity() == null) {
            throw new InvalidVenueException("City is required");
        }
        if (venue.getCountry() == null) {
            throw new InvalidVenueException("Country is required");
        }
        if (venue.getPostalCode() == null) {
            throw new InvalidVenueException("Postal code is required");
        }

        //venue.setCreatedBy(new UUID(55,5));
        if (venue.getCreatedBy() == null && venue.getId() == null) {
            throw new InvalidVenueException("CreatedBy is required");
        }
    }

    @Transactional
    public Venue createVenue(Venue venue) {
        validateVenue(venue);

        if (venuesRepository.existsByName(venue.getName())) {
            throw new InvalidVenueException("Venue with name " + venue.getName() + " already exists");
        }

        venue.setActive(true);
        return venuesRepository.save(venue);
    }

    @Transactional
    public Venue deactivateVenue(UUID id) {
        Venue venue = venuesRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException("Venue not found"));

        if (!venue.isActive()) {
            throw new VenueAlreadyDeactivatedException("Venue already deactivated");
        }

        venue.setActive(false);
        return venuesRepository.save(venue);
    }

    public Venue getVenue(UUID id) {
        return venuesRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException("Venue not found"));
    }

    public List<Venue> searchVenues(VenueSearchDTO criteria) {
        if (criteria.getMinCapacity() != null && criteria.getMinCapacity() < 0) {
            throw new InvalidVenueException("minCapacity must be >= 0");
        }

        Specification<Venue> spec = Specification.where(null);

        if (criteria.getOnlyActive() != null && criteria.getOnlyActive()) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("active")));
        }

        if (criteria.getName() != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + criteria.getName().toLowerCase() + "%"));
        }

        if (criteria.getCity() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("city")), criteria.getCity().toLowerCase()));
        }

        if (criteria.getCountry() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("country")), criteria.getCountry().toLowerCase()));
        }

        if (criteria.getMinCapacity() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("capacity"), criteria.getMinCapacity()));
        }

        return venuesRepository.findAll(spec);
    }
}

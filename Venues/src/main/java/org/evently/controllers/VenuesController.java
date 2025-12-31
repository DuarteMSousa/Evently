package org.evently.controllers;

import org.evently.dtos.venues.VenueCreateDTO;
import org.evently.dtos.venues.VenueDTO;
import org.evently.dtos.venues.VenueSearchDTO;
import org.evently.exceptions.InvalidVenueException;
import org.evently.exceptions.VenueAlreadyDeactivatedException;
import org.evently.exceptions.VenueNotFoundException;
import org.evently.models.Venue;
import org.evently.service.VenuesService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/venues")
public class VenuesController {

    private static final Logger logger = LoggerFactory.getLogger(VenuesController.class);

    private static final Marker VENUE_CREATE = MarkerFactory.getMarker("VENUE_CREATE");
    private static final Marker VENUE_DEACTIVATE = MarkerFactory.getMarker("VENUE_DEACTIVATE");
    private static final Marker VENUE_GET = MarkerFactory.getMarker("VENUE_GET");
    private static final Marker VENUE_SEARCH = MarkerFactory.getMarker("VENUE_SEARCH");

    @Autowired
    private VenuesService venuesService;

    private final ModelMapper modelMapper;

    public VenuesController() {
        this.modelMapper = new ModelMapper();
    }

    @GetMapping("/get-venue/{id}")
    public ResponseEntity<?> getVenue(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Venue found.
         * 404 NOT_FOUND - Venue does not exist.
         * 400 BAD_REQUEST - Generic error.
         */
        logger.info(VENUE_GET, "GET /venues/get-venue/{} requested", id);

        try {
            Venue venue = venuesService.getVenue(id);

            logger.info(VENUE_GET, "GET venue succeeded (id={}, name={})", venue.getId(), venue.getName());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(modelMapper.map(venue, VenueDTO.class));

        } catch (VenueNotFoundException e) {
            logger.warn(VENUE_GET, "GET venue failed - not found (id={})", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (Exception e) {
            logger.error(VENUE_GET, "GET venue failed - unexpected error (id={})", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/create-venue")
    public ResponseEntity<?> createVenue(@RequestBody VenueCreateDTO dto) {
        /* HttpStatus(produces)
         * 201 CREATED - Venue created successfully.
         * 400 BAD_REQUEST - Invalid data provided.
         */
        logger.info(VENUE_CREATE,
                "POST /venues/create-venue requested (name={}, city={}, country={})",
                dto.getName(), dto.getCity(), dto.getCountry());

        try {
            Venue venue = modelMapper.map(dto, Venue.class);
            Venue newVenue = venuesService.createVenue(venue);

            logger.info(VENUE_CREATE, "Create venue succeeded (id={}, name={})",
                    newVenue.getId(), newVenue.getName());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(modelMapper.map(newVenue, VenueDTO.class));

        } catch (InvalidVenueException e) {
            logger.warn(VENUE_CREATE, "Create venue failed - invalid payload (name={}) reason={}",
                    dto.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            logger.error(VENUE_CREATE, "Create venue failed - unexpected error (name={})",
                    dto.getName(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/search-venues")
    public ResponseEntity<?> searchVenues(@RequestBody VenueSearchDTO criteria) {
        /* HttpStatus(produces)
         * 200 OK - Venues found (list returned).
         * 400 BAD_REQUEST - Invalid search criteria.
         */
        logger.info(VENUE_SEARCH,
                "POST /venues/search-venues requested (onlyActive={}, name={}, city={}, country={}, minCapacity={})",
                criteria.getOnlyActive(),
                criteria.getName(),
                criteria.getCity(),
                criteria.getCountry(),
                criteria.getMinCapacity()
        );

        try {
            List<VenueDTO> results = venuesService.searchVenues(criteria)
                    .stream()
                    .map(v -> modelMapper.map(v, VenueDTO.class))
                    .collect(Collectors.toList());

            logger.info(VENUE_SEARCH, "Search venues succeeded (results={})", results.size());

            return ResponseEntity.status(HttpStatus.OK).body(results);

        } catch (InvalidVenueException e) {
            logger.warn(VENUE_SEARCH, "Search venues failed - invalid criteria reason={}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            logger.error(VENUE_SEARCH, "Search venues failed - unexpected error", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/deactivate-venue/{id}")
    public ResponseEntity<?> deactivateVenue(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Venue deactivated successfully.
         * 404 NOT_FOUND - Venue does not exist.
         * 409 CONFLICT - Venue is already deactivated.
         * 400 BAD_REQUEST - Generic error.
         */
        logger.info(VENUE_DEACTIVATE, "PUT /venues/deactivate-venue/{} requested", id);

        try {
            Venue updatedVenue = venuesService.deactivateVenue(id);

            logger.info(VENUE_DEACTIVATE, "Deactivate venue succeeded (id={})", updatedVenue.getId());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(modelMapper.map(updatedVenue, VenueDTO.class));

        } catch (VenueNotFoundException e) {
            logger.warn(VENUE_DEACTIVATE, "Deactivate venue failed - not found (id={})", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (VenueAlreadyDeactivatedException e) {
            logger.warn(VENUE_DEACTIVATE, "Deactivate venue failed - already deactivated (id={})", id);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());

        } catch (Exception e) {
            logger.error(VENUE_DEACTIVATE, "Deactivate venue failed - unexpected error (id={})", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}

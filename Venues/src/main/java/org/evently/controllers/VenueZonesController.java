package org.evently.controllers;

import org.evently.dtos.venueszone.VenueZoneCreateDTO;
import org.evently.dtos.venueszone.VenueZoneDTO;
import org.evently.dtos.venueszone.VenueZoneUpdateDTO;
import org.evently.exceptions.InvalidVenueZoneException;
import org.evently.exceptions.VenueNotFoundException;
import org.evently.exceptions.VenueZoneNotFoundException;
import org.evently.models.VenueZone;
import org.evently.service.VenueZonesService;
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
@RequestMapping("/venues/zones")
public class VenueZonesController {

    private static final Logger logger = LoggerFactory.getLogger(VenueZonesController.class);

    private static final Marker ZONE_CREATE = MarkerFactory.getMarker("VENUE_ZONE_CREATE");
    private static final Marker ZONE_GET = MarkerFactory.getMarker("VENUE_ZONE_GET");
    private static final Marker ZONE_LIST = MarkerFactory.getMarker("VENUE_ZONE_LIST");
    private static final Marker ZONE_UPDATE = MarkerFactory.getMarker("VENUE_ZONE_UPDATE");

    @Autowired
    private VenueZonesService venueZonesService;

    private final ModelMapper modelMapper;

    public VenueZonesController() {
        this.modelMapper = new ModelMapper();
    }

    private VenueZoneDTO toDTO(VenueZone zone) {
        VenueZoneDTO dto = modelMapper.map(zone, VenueZoneDTO.class);
        if (zone.getVenue() != null) {
            dto.setVenueId(zone.getVenue().getId());
            dto.setVenueName(zone.getVenue().getName());
        }
        return dto;
    }

    @PostMapping("/create-zone")
    public ResponseEntity<?> createZone(@RequestBody VenueZoneCreateDTO dto) {
        /* HttpStatus(produces)
         * 201 CREATED - Zone created successfully.
         * 404 NOT_FOUND - Venue does not exist.
         * 400 BAD_REQUEST - Invalid data provided.
         */
        logger.info(ZONE_CREATE,
                "POST /venues/zones/create-zone requested (venueId={}, name={}, capacity={})",
                dto.getVenueId(), dto.getName(), dto.getCapacity());

        try {
            VenueZone zone = modelMapper.map(dto, VenueZone.class);

            VenueZone newZone = venueZonesService.createVenueZone(dto.getVenueId(), zone);

            logger.info(ZONE_CREATE, "Create zone succeeded (venueId={}, zoneId={}, name={})",
                    dto.getVenueId(), newZone.getId(), newZone.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(newZone));

        } catch (VenueNotFoundException e) {
            logger.warn(ZONE_CREATE, "Create zone failed - venue not found (venueId={})", dto.getVenueId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (InvalidVenueZoneException e) {
            logger.warn(ZONE_CREATE, "Create zone failed - invalid payload (venueId={}) reason={}",
                    dto.getVenueId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            logger.error(ZONE_CREATE, "Create zone failed - unexpected error (venueId={})", dto.getVenueId(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/get-zone/{id}")
    public ResponseEntity<?> getZone(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Zone found.
         * 404 NOT_FOUND - Zone does not exist.
         * 400 BAD_REQUEST - Generic error.
         */
        logger.info(ZONE_GET, "GET /venues/zones/get-zone/{} requested", id);

        try {
            VenueZone zone = venueZonesService.getVenueZone(id);

            logger.info(ZONE_GET, "Get zone succeeded (zoneId={}, name={})", zone.getId(), zone.getName());

            return ResponseEntity.status(HttpStatus.OK).body(toDTO(zone));

        } catch (VenueZoneNotFoundException e) {
            logger.warn(ZONE_GET, "Get zone failed - not found (zoneId={})", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (Exception e) {
            logger.error(ZONE_GET, "Get zone failed - unexpected error (zoneId={})", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/by-venue/{venueId}")
    public ResponseEntity<?> getZonesByVenue(@PathVariable("venueId") UUID venueId) {
        /* HttpStatus(produces)
         * 200 OK - List of zones for the specified venue retrieved successfully.
         * 404 NOT_FOUND - Venue does not exist.
         * 400 BAD_REQUEST - Generic error.
         */
        logger.info(ZONE_LIST, "GET /venues/zones/by-venue/{} requested", venueId);

        try {
            List<VenueZoneDTO> zones = venueZonesService.getVenueZonesByVenue(venueId)
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

            logger.info(ZONE_LIST, "List zones succeeded (venueId={}, results={})", venueId, zones.size());

            return ResponseEntity.status(HttpStatus.OK).body(zones);

        } catch (VenueNotFoundException e) {
            logger.warn(ZONE_LIST, "List zones failed - venue not found (venueId={})", venueId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (Exception e) {
            logger.error(ZONE_LIST, "List zones failed - unexpected error (venueId={})", venueId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/update-zone/{id}")
    public ResponseEntity<?> updateZone(@PathVariable("id") UUID id,
                                        @RequestBody VenueZoneUpdateDTO dto) {
        /* HttpStatus(produces)
         * 200 OK - Zone updated successfully.
         * 404 NOT_FOUND - Zone does not exist OR venue does not exist (depending on service validation).
         * 400 BAD_REQUEST - Invalid data provided.
         */
        logger.info(ZONE_UPDATE,
                "PUT /venues/zones/update-zone/{} requested (bodyId={}, name={}, capacity={}, updatedBy={})",
                id, dto.getId(), dto.getName(), dto.getCapacity(), dto.getUpdatedBy());

        try {
            VenueZone zone = modelMapper.map(dto, VenueZone.class);

            VenueZone updated = venueZonesService.updateVenueZone(id, zone);

            logger.info(ZONE_UPDATE, "Update zone succeeded (zoneId={}, name={}, capacity={})",
                    updated.getId(), updated.getName(), updated.getCapacity());

            return ResponseEntity.status(HttpStatus.OK).body(toDTO(updated));

        } catch (VenueZoneNotFoundException e) {
            logger.warn(ZONE_UPDATE, "Update zone failed - zone not found (zoneId={})", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (VenueNotFoundException e) {
            logger.warn(ZONE_UPDATE, "Update zone failed - venue not found (zoneId={})", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (InvalidVenueZoneException e) {
            logger.warn(ZONE_UPDATE, "Update zone failed - invalid payload (zoneId={}) reason={}",
                    id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            logger.error(ZONE_UPDATE, "Update zone failed - unexpected error (zoneId={})", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

}

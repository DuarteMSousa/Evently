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
        /*
         * 201 CREATED – Zona criada
         * 400 BAD_REQUEST – Campos inválidos
         * 404 NOT_FOUND – Venue não encontrado
         */
        try {
            VenueZone zone = modelMapper.map(dto, VenueZone.class);

            VenueZone newZone = venueZonesService.createVenueZone(dto.getVenueId(), zone);

            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(newZone));
        } catch (VenueNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidVenueZoneException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/get-zone/{id}")
    public ResponseEntity<?> getZone(@PathVariable("id") UUID id) {
        try {
            VenueZone zone = venueZonesService.getVenueZone(id);
            return ResponseEntity.status(HttpStatus.OK).body(toDTO(zone));
        } catch (VenueZoneNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/by-venue/{venueId}")
    public ResponseEntity<?> getZonesByVenue(@PathVariable("venueId") UUID venueId) {
        try {
            List<VenueZoneDTO> zones = venueZonesService.getVenueZonesByVenue(venueId)
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK).body(zones);
        } catch (VenueNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/update-zone/{id}")
    public ResponseEntity<?> updateZone(@PathVariable("id") UUID id,
                                        @RequestBody VenueZoneUpdateDTO dto) {
        try {
            VenueZone zone = modelMapper.map(dto, VenueZone.class);

            VenueZone updated = venueZonesService.updateVenueZone(id, zone);

            return ResponseEntity.status(HttpStatus.OK).body(toDTO(updated));
        } catch (VenueZoneNotFoundException | VenueNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidVenueZoneException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
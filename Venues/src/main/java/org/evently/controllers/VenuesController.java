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

    @Autowired
    private VenuesService venuesService;

    private final ModelMapper modelMapper;

    public VenuesController() {
        this.modelMapper = new ModelMapper();
    }

    @GetMapping("/get-venue/{id}")
    public ResponseEntity<?> getVenue(@PathVariable("id") UUID id) {
        /*
         * 200 OK - Local encontrado
         * 404 NOT_FOUND - Local não encontrado
         * 400 BAD_REQUEST - Erro genérico
         */
        try {
            Venue venue = venuesService.getVenue(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(modelMapper.map(venue, VenueDTO.class));
        } catch (VenueNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/create-venue")
    public ResponseEntity<?> createVenue(@RequestBody VenueCreateDTO dto) {
        /*
         * 201 CREATED - Local registado
         * 400 BAD_REQUEST - Campos inválidos
         */
        try {
            Venue venue = modelMapper.map(dto, Venue.class);
            Venue newVenue = venuesService.createVenue(venue);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(modelMapper.map(newVenue, VenueDTO.class));
        } catch (InvalidVenueException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/search-venues")
    public ResponseEntity<?> searchVenues(@RequestBody VenueSearchDTO criteria) {
        /*
         * 200 OK - Locais encontrados
         * 400 BAD_REQUEST - Campos inválidos
         */
        try {
            List<VenueDTO> results = venuesService.searchVenues(criteria)
                    .stream()
                    .map(v -> modelMapper.map(v, VenueDTO.class))
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK).body(results);
        } catch (InvalidVenueException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/deactivate-venue/{id}")
    public ResponseEntity<?> deactivateVenue(@PathVariable("id") UUID id) {
        /*
         * 200 OK - Local desativado
         * 404 NOT_FOUND - Local não encontrado
         * 409 CONFLICT - Local já desativado
         * 400 BAD_REQUEST - Erro genérico
         */
        try {
            Venue updatedVenue = venuesService.deactivateVenue(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(modelMapper.map(updatedVenue, VenueDTO.class));
        } catch (VenueNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (VenueAlreadyDeactivatedException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}

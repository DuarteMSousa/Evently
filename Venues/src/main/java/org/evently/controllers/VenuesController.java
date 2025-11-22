package org.evently.controllers;

import org.evently.models.Venue;
import org.evently.service.VenuesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class VenuesController {

    private final VenuesService venuesService;

    public VenuesController(VenuesService venuesService) {
        this.venuesService = venuesService;
    }

    // GET /get-venue/{venueId}
    @GetMapping("/get-venue/{venueId}")
    public ResponseEntity<Venue> getVenue(@PathVariable("venueId") UUID venueId) {
        Venue venue = venuesService.getVenue(venueId);

        if (venue == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(venue);
    }

    // POST /search-venues
    @PostMapping("/search-venues")
    public ResponseEntity<List<Venue>> searchVenues(@RequestBody Map<String, Object> body) {

        try {
            String name = (String) body.getOrDefault("name", null);
            String city = (String) body.getOrDefault("city", null);
            String country = (String) body.getOrDefault("country", null);
            Boolean active = (body.get("active") != null) ? (Boolean) body.get("active") : null;

            List<Venue> result = venuesService.searchVenues(name, city, country, active);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // POST /create-venue
    @PostMapping("/create-venue")
    public ResponseEntity<?> createVenue(@RequestBody Venue venue) {
        try {
            UUID id = venuesService.createVenue(venue);

            Map<String, Object> body = new HashMap<>();
            body.put("id", id);
            body.put("message", "Venue created");

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(body);
        } catch (IllegalArgumentException ex) {
            Map<String, String> error = new HashMap<>();
            error.put("message", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(error);
        }
    }

    // PUT /deactivate-venue/{venueId}
    @PutMapping("/deactivate-venue/{venueId}")
    public ResponseEntity<Map<String, String>> deactivateVenue(
            @PathVariable("venueId") UUID venueId) {

        boolean ok = venuesService.deactivateVenue(venueId);

        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Venue not found"));
        }

        return ResponseEntity.ok(Map.of("message", "Venue deactivated"));
    }
}

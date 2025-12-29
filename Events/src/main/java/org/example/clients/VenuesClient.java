package org.example.clients;

import org.example.dtos.externalServices.venues.VenueDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "venues", path = "/venues")
public interface VenuesClient {

    @GetMapping("/get-venue/{id}")
    ResponseEntity<VenueDTO> getVenue(@PathVariable("id") UUID id);


    @GetMapping("/zones/get-zone/{id}")
     ResponseEntity<?> getZone(@PathVariable("id") UUID id);
}

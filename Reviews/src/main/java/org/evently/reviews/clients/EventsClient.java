package org.evently.reviews.clients;

import org.evently.reviews.dtos.externalServices.EventDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "events", path = "/events")
public interface EventsClient {

    @GetMapping("/get-event/{id}")
    ResponseEntity<EventDTO> searchEvents(@PathVariable("id") UUID id);
}

package org.example.clients;

import org.example.dtos.EventDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "events", path = "/events")
public interface EventsClient {

    @GetMapping("/get-event/{id}")
    ResponseEntity<EventDTO> getEvent(@PathVariable("id") UUID id);

}

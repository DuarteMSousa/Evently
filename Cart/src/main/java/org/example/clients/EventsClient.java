package org.example.clients;


import org.example.dtos.externalServices.eventSessions.EventSessionDTO;
import org.example.dtos.externalServices.events.EventDTO;
import org.example.dtos.externalServices.sessionTiers.SessionTierDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "events", path = "/events")
public interface EventsClient {

    @GetMapping("/get-event/{id}")
    ResponseEntity<EventDTO> getEvent(@PathVariable("id") UUID id);

    @GetMapping("/sessions/get-event-session/{id}")
    ResponseEntity<EventSessionDTO> getEventSession(@PathVariable("id") UUID id);

    @GetMapping("/sessions/tiers/get-session-tier/{id}")
    ResponseEntity<SessionTierDTO> getSessionTier(@PathVariable("id") UUID id);
}

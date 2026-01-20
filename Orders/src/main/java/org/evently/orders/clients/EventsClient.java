package org.evently.orders.clients;

import org.evently.orders.dtos.externalServices.events.EventSessionDTO;
import org.evently.orders.dtos.externalServices.events.SessionTierDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "events", path = "/events")
public interface EventsClient {

    @GetMapping("/sessions/get-event-session/{id}")
    ResponseEntity<EventSessionDTO> getEventSession(@PathVariable("id") UUID id);

    @GetMapping("/sessions/tiers/get-session-tier/{id}")
    ResponseEntity<SessionTierDTO> getSessionTier(@PathVariable("id") UUID id);

}

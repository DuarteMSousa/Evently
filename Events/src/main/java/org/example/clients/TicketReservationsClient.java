package org.example.clients;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "ticketReservations", path = "/ticketReservations")
public interface TicketReservationsClient {

    @GetMapping("/event-has-reservations/{eventId}")
    public ResponseEntity<Boolean> checkEventReservations(@PathVariable("eventId") UUID eventId);

    @GetMapping("/session-has-reservations/{sessionId}")
    public ResponseEntity<Boolean> checkSessionReservations(@PathVariable("sessionId") UUID sessionId);

    @GetMapping("/tier-has-reservations/{tierId}")
    public ResponseEntity<Boolean> checkTierReservations(@PathVariable("tierId") UUID tierId);

}

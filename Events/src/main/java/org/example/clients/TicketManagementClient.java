package org.example.clients;

import org.example.dtos.externalServices.ticketStocks.TicketStockCreateDTO;
import org.example.dtos.externalServices.ticketStocks.TicketStockDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "ticketManagement", path = "/ticketManagement")
public interface TicketManagementClient {

    @GetMapping("/ticketReservations/event-has-reservations/{eventId}")
    ResponseEntity<Boolean> checkEventReservations(@PathVariable("eventId") UUID eventId);

    @GetMapping("/ticketReservations/session-has-reservations/{sessionId}")
    ResponseEntity<Boolean> checkSessionReservations(@PathVariable("sessionId") UUID sessionId);

    @GetMapping("/ticketReservations/tier-has-reservations/{tierId}")
    ResponseEntity<Boolean> checkTierReservations(@PathVariable("tierId") UUID tierId);

    @PostMapping("/ticketStocks/create-stock")
    ResponseEntity<TicketStockDTO> createTicketStock(@RequestBody TicketStockCreateDTO ticketStock);

    @DeleteMapping("/ticketStocks/delete-event-stock/{eventId}")
    ResponseEntity<?> deleteEventTicketStock(@PathVariable("eventId") UUID eventId);

    @DeleteMapping("/ticketStocks/delete-session-stock/{sessionId}")
    ResponseEntity<?> deleteSessionTicketStock(@PathVariable("sessionId") UUID sessionId);

    @DeleteMapping("/ticketStocks/delete-tier-stock/{tierId}")
    ResponseEntity<?> deleteTierTicketStock(@PathVariable("tierId") UUID tierId);

}

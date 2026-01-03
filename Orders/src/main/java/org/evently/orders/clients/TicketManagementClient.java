package org.evently.orders.clients;

import org.evently.orders.dtos.externalServices.TicketReservationCreateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ticketManagement", path = "/ticketManagement")
public interface TicketManagementClient {

    @PostMapping("/ticketReservations/reserve-ticket")
    public ResponseEntity<?> reserveTicket(@RequestBody TicketReservationCreateDTO reservationDto);
}

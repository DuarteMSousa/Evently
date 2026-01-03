package org.evently.orders.clients;

import org.evently.orders.dtos.externalServices.TicketReservationCreateDTO;
import org.evently.orders.dtos.externalServices.TicketReservationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ticketManagement", path = "/ticketManagement")
public interface TicketManagementClient {

    @PostMapping("/ticketReservations/reserve-ticket")
    ResponseEntity<TicketReservationDTO> reserveTicket(@RequestBody TicketReservationCreateDTO reservationDto);
}

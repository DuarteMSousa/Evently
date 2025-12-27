package org.evently.tickets.controllers;

import org.evently.tickets.dtos.TicketCreateDTO;
import org.evently.tickets.dtos.TicketDTO;
import org.evently.tickets.enums.TicketStatus;
import org.evently.tickets.exceptions.InvalidTicketUpdateException;
import org.evently.tickets.exceptions.TicketNotFoundException;
import org.evently.tickets.models.Ticket;
import org.evently.tickets.services.TicketsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tickets")
public class TicketsController {

    @Autowired
    private TicketsService ticketsService;

    @GetMapping("/get-ticket/{id}")
    public ResponseEntity<?> getTicket(@PathVariable("id") UUID id) {
        /*
         * 200 OK - Ticket found.
         * 404 NOT_FOUND - No ticket exists with the provided ID.
         * 400 BAD_REQUEST - Unexpected error.
         */
        try {
            Ticket ticket = ticketsService.getTicket(id);
            return ResponseEntity.ok(convertToDTO(ticket));
        } catch (TicketNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<TicketDTO>> getTicketsByUser(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        /*
         * 200 OK - Paginated list of tickets for user retrieved successfully.
         */
        Page<Ticket> ticketPage = ticketsService.getTicketsByUser(userId, page, size);
        Page<TicketDTO> dtoPage = ticketPage.map(this::convertToDTO);
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/register-ticket")
    public ResponseEntity<?> registerTicket(@RequestBody TicketCreateDTO ticketDTO) {
        /*
         * 201 CREATED - Ticket registered successfully.
         * 400 BAD_REQUEST - Validation error or missing fields.
         */
        try {
            Ticket ticketRequest = new Ticket();
            ticketRequest.setReservationId(ticketDTO.getReservationId());
            ticketRequest.setOrderId(ticketDTO.getOrderId());
            ticketRequest.setUserId(ticketDTO.getUserId());
            ticketRequest.setEventId(ticketDTO.getEventId());
            ticketRequest.setSessionId(ticketDTO.getSessionId());
            ticketRequest.setTierId(ticketDTO.getTierId());
            ticketRequest.setStatus(TicketStatus.ISSUED);

            Ticket savedTicket = ticketsService.registerTicket(ticketRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedTicket));
        } catch (InvalidTicketUpdateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

//    @PutMapping("/update-ticket/{id}")
//    public ResponseEntity<?> updateTicket(@PathVariable("id") UUID id, @RequestBody TicketCreateDTO ticketDTO) {
//        /*
//         * 200 OK - Ticket updated successfully.
//         * 404 NOT_FOUND - Ticket not found.
//         * 400 BAD_REQUEST - ID mismatch or validation error.
//         */
//        try {
//            Ticket updateData = new Ticket();
//            updateData.setReservationId(ticketDTO.getReservationId());
//            updateData.setOrderId(ticketDTO.getOrderId());
//            updateData.setUserId(ticketDTO.getUserId());
//            updateData.setEventId(ticketDTO.getEventId());
//            updateData.setSessionId(ticketDTO.getSessionId());
//            updateData.setTierId(ticketDTO.getTierId());
//            updateData.setStatus(ticketDTO.getStatus());
//
//            Ticket updated = ticketsService.updateTicket(id, updateData);
//            return ResponseEntity.ok(convertToDTO(updated));
//        } catch (TicketNotFoundException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
//        } catch (InvalidTicketUpdateException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        }
//    }

    private TicketDTO convertToDTO(Ticket ticket) {
        TicketDTO dto = new TicketDTO();
        dto.setId(ticket.getId());
        dto.setReservationId(ticket.getReservationId());
        dto.setOrderId(ticket.getOrderId());
        dto.setUserId(ticket.getUserId());
        dto.setEventId(ticket.getEventId());
        dto.setSessionId(ticket.getSessionId());
        dto.setTierId(ticket.getTierId());
        dto.setStatus(ticket.getStatus());
        dto.setIssuedAt(ticket.getIssuedAt());
        dto.setValidatedAt(ticket.getValidatedAt());
        return dto;
    }
}
package org.evently.tickets.controllers;

import org.evently.tickets.dtos.TicketCreateDTO;
import org.evently.tickets.dtos.TicketDTO;
import org.evently.tickets.enums.TicketStatus;
import org.evently.tickets.exceptions.InvalidTicketUpdateException;
import org.evently.tickets.exceptions.TicketNotFoundException;
import org.evently.tickets.models.Ticket;
import org.evently.tickets.services.TicketsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
public class TicketsController {

    private static final Logger logger = LoggerFactory.getLogger(TicketsController.class);

    private static final Marker TICKET_GET = MarkerFactory.getMarker("TICKET_GET");
    private static final Marker TICKET_CREATE = MarkerFactory.getMarker("TICKET_CREATE");
    private static final Marker TICKET_CANCEL = MarkerFactory.getMarker("TICKET_CANCEL");
    private static final Marker TICKET_USE = MarkerFactory.getMarker("TICKET_USE");

    @Autowired
    private TicketsService ticketsService;

    @GetMapping("/get-ticket/{id}")
    public ResponseEntity<?> getTicket(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Ticket found.
         * 404 NOT_FOUND - No ticket exists with the provided ID.
         * 400 BAD_REQUEST - Unexpected error.
         */

        logger.info(TICKET_GET, "Method getTicket entered for id: {}", id);
        try {
            Ticket ticket = ticketsService.getTicket(id);
            logger.info(TICKET_GET, "200 OK returned, ticket found");
            return ResponseEntity.ok(convertToDTO(ticket));
        } catch (TicketNotFoundException e) {
            logger.error(TICKET_GET, "TicketNotFoundException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(TICKET_GET, "Unexpected exception caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}/{pageNumber}/{pageSize}")
    public ResponseEntity<Page<TicketDTO>> getTicketsByUser(
            @PathVariable("userId") UUID userId,
            @PathVariable("pageNumber") Integer pageNumber, @PathVariable("pageSize") Integer pageSize) {
        /* HttpStatus(produces)
         * 200 OK - Paginated list of tickets for user retrieved successfully.
         */

        logger.info(TICKET_GET, "Method getTicketsByUser entered for userId: {} (page={}, size={})", userId, pageNumber, pageSize);

        Page<Ticket> ticketPage = ticketsService.getTicketsByUser(userId, pageNumber, pageSize);
        Page<TicketDTO> dtoPage = ticketPage.map(this::convertToDTO);

        logger.info(TICKET_GET, "200 OK returned, paginated tickets retrieved");
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/issue-ticket")
    public ResponseEntity<?> issueTicket(@RequestBody TicketCreateDTO ticketDTO) {
        /* HttpStatus(produces)
         * 201 CREATED - Ticket registered successfully.
         * 400 BAD_REQUEST - Validation error or missing fields.
         */

        logger.info(TICKET_CREATE, "Method issueTicket entered for userId: {}, eventId: {}",
                ticketDTO.getUserId(), ticketDTO.getEventId());
        try {
            Ticket ticketRequest = new Ticket();
            ticketRequest.setReservationId(ticketDTO.getReservationId());
            ticketRequest.setOrderId(ticketDTO.getOrderId());
            ticketRequest.setUserId(ticketDTO.getUserId());
            ticketRequest.setEventId(ticketDTO.getEventId());
            ticketRequest.setSessionId(ticketDTO.getSessionId());
            ticketRequest.setTierId(ticketDTO.getTierId());
            ticketRequest.setStatus(TicketStatus.ISSUED);
            ticketRequest.setIssuedAt(new Date());

            Ticket savedTicket = ticketsService.issueTicket(ticketRequest);
            logger.info(TICKET_CREATE, "201 CREATED returned, ticket issued with id: {}", savedTicket.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedTicket));
        } catch (Exception e) {
            logger.error(TICKET_CREATE, "Exception caught while issuing ticket: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/use-ticket/{id}")
    public ResponseEntity<?> useTicket(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Ticket validated/used successfully.
         * 404 NOT_FOUND - No ticket exists with the provided ID.
         * 400 BAD_REQUEST - Ticket is already used or cancelled.
         */

        logger.info(TICKET_USE, "Method useTicket entered for id: {}", id);
        try {
            Ticket validatedTicket = ticketsService.useTicket(id);
            logger.info(TICKET_USE, "200 OK returned, ticket marked as USED");
            return ResponseEntity.ok(convertToDTO(validatedTicket));
        } catch (TicketNotFoundException e) {
            logger.error(TICKET_USE, "TicketNotFoundException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidTicketUpdateException e) {
            logger.error(TICKET_USE, "InvalidTicketUpdateException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/cancel-ticket/{id}")
    public ResponseEntity<?> cancelTicket(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Ticket cancelled successfully.
         * 404 NOT_FOUND - No ticket exists with the provided ID.
         * 400 BAD_REQUEST - Ticket is already used or already cancelled.
         */

        logger.info(TICKET_CANCEL, "Method cancelTicket entered for id: {}", id);
        try {
            Ticket cancelledTicket = ticketsService.cancelTicket(id);
            logger.info(TICKET_CANCEL, "200 OK returned, ticket marked as CANCELLED");
            return ResponseEntity.ok(convertToDTO(cancelledTicket));
        } catch (TicketNotFoundException e) {
            logger.error(TICKET_CANCEL, "TicketNotFoundException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidTicketUpdateException e) {
            logger.error(TICKET_CANCEL, "InvalidTicketUpdateException caught: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

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
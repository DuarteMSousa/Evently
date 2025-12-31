package org.example.controllers;


import org.example.dtos.TicketReservationCreateDTO;
import org.example.dtos.TicketReservationDTO;
import org.example.models.TicketReservation;
import org.example.services.TicketReservationsService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/ticketManagement/ticketReservations")
public class TicketReservationsController {

    @Autowired
    private TicketReservationsService ticketReservationsService;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(TicketReservationsController.class);

    private static final Marker TICKET_RESERVATION_CREATE = MarkerFactory.getMarker("TICKET_RESERVATION_CREATE");

    private static final Marker EVENT_RESERVATIONS_CHECK = MarkerFactory.getMarker("EVENT_RESERVATIONS_CHECK");

    private static final Marker SESSION_RESERVATIONS_CHECK = MarkerFactory.getMarker("SESSION_RESERVATIONS_CHECK");

    private static final Marker TIER_RESERVATIONS_CHECK = MarkerFactory.getMarker("TIER_RESERVATIONS_CHECK");

    @PostMapping("/reserve-ticket")
    public ResponseEntity<?> reserveTicket(@RequestBody TicketReservationCreateDTO reservationDto) {
        /* HttpStatus(produces)
         * 201 CREATED - Ticket reservation created successfully.
         * 400 BAD_REQUEST - Validation error or missing fields.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(TICKET_RESERVATION_CREATE, "reserveTicket method entered");
        TicketReservation ticketReservation = modelMapper.map(reservationDto, TicketReservation.class);
        TicketReservation savedReservation;
        try {
            savedReservation = ticketReservationsService.createTicketReservation(ticketReservation);

        } catch (Exception e) {
            logger.error(TICKET_RESERVATION_CREATE, "Exception caught while saving ticket Reservation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        TicketReservationDTO returnDto = modelMapper.map(savedReservation, TicketReservationDTO.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(returnDto);
    }

    @GetMapping("/event-has-reservations/{eventId}")
    public ResponseEntity<?> checkEventReservations(@PathVariable("eventId") UUID eventId) {
        /* HttpStatus(produces)
         * 201 CREATED - Ticket reservation created successfully.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(EVENT_RESERVATIONS_CHECK, "checkEventReservations method entered");
        boolean result;
        try {
            result = ticketReservationsService.eventHasReservations(eventId);
        } catch (Exception e) {
            logger.error(EVENT_RESERVATIONS_CHECK, "Exception caught while checking event reservations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/session-has-reservations/{sessionId}")
    public ResponseEntity<?> checkSessionReservations(@PathVariable("sessionId") UUID sessionId) {
        /* HttpStatus(produces)
         * 201 CREATED - Ticket reservation created successfully.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(SESSION_RESERVATIONS_CHECK, "checkSessionReservations method entered");
        boolean result;
        try {
            result = ticketReservationsService.sessionHasReservations(sessionId);
        } catch (Exception e) {
            logger.error(SESSION_RESERVATIONS_CHECK, "Exception caught while checking session reservations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/tier-has-reservations/{tierId}")
    public ResponseEntity<?> checkTierReservations(@PathVariable("tierId") UUID tierId) {
        /* HttpStatus(produces)
         * 201 CREATED - Ticket reservation created successfully.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(TIER_RESERVATIONS_CHECK, "checkTierReservations method entered");
        boolean result;
        try {
            result = ticketReservationsService.tierHasReservations(tierId);
        } catch (Exception e) {
            logger.error(TIER_RESERVATIONS_CHECK, "Exception caught while checking tier reservations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

}

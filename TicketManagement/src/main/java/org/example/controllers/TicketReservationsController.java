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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ticketManagement/ticketReservations")
public class TicketReservationsController {

    @Autowired
    private TicketReservationsService ticketReservationsService;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(TicketReservationsController.class);

    private Marker reserveMarker = MarkerFactory.getMarker("ReserveTicket");

    private static final Marker TICKET_RESERVATION_CREATE = MarkerFactory.getMarker("TICKET_RESERVATION_CREATE");


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
            logger.error(TICKET_RESERVATION_CREATE, "Exception caught while saving ticket Reservation: {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        TicketReservationDTO returnDto = modelMapper.map(savedReservation, TicketReservationDTO.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(returnDto);
    }

}

package org.example.services;

import jakarta.transaction.Transactional;
import org.example.enums.StockMovementType;
import org.example.enums.TicketReservationStatus;
import org.example.exceptions.InvalidTicketReservationException;
import org.example.exceptions.TicketReservationNotFoundException;
import org.example.exceptions.TicketStockNotFoundException;
import org.example.models.StockMovement;
import org.example.models.TicketReservation;
import org.example.models.TicketStock;
import org.example.models.TicketStockId;
import org.example.repositories.TicketReservationsRepository;
import org.example.repositories.TicketStocksRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class TicketReservationsService {

    @Autowired
    private TicketReservationsRepository ticketReservationsRepository;

    @Autowired
    private TicketStocksService ticketStocksService;

    @Autowired
    private TicketStocksRepository ticketStocksRepository;

    private static final Logger logger = LoggerFactory.getLogger(TicketReservationsService.class);

    private static final Marker TICKET_RESERVATION_CREATE = MarkerFactory.getMarker("TICKET_RESERVATION_CREATE");
    private static final Marker TICKET_RESERVATION_CONFIRM = MarkerFactory.getMarker("TICKET_RESERVATION_CONFIRM");
    private static final Marker TICKET_RESERVATION_GET = MarkerFactory.getMarker("TICKET_RESERVATION_GET");
    private static final Marker TICKET_RESERVATION_RELEASE = MarkerFactory.getMarker("TICKET_RESERVATION_RELEASE");

    @Transactional
    public TicketReservation createTicketReservation(TicketReservation ticketReservation) {

        logger.info(TICKET_RESERVATION_CREATE, "createTicketReservation method entered");

        ticketReservation.setStatus(TicketReservationStatus.HELD);
        ticketReservation.setExpiresAt(null);
        ticketReservation.setReleasedAt(null);
        ticketReservation.setConfirmedAt(null);

        if (ticketReservation.getQuantity() <= 0) {
            logger.info(TICKET_RESERVATION_CREATE, "Quantity must be greater than 0");
            throw new InvalidTicketReservationException("Quantity must be greater than 0");
        }

        createStockMovement(ticketReservation, StockMovementType.OUT);

        return ticketReservationsRepository.save(ticketReservation);
    }

    @Transactional
    public TicketReservation confirmTicketReservation(UUID id) {
        logger.info(TICKET_RESERVATION_CONFIRM, "confirmTicketReservation method entered");
        TicketReservation ticketReservation = ticketReservationsRepository.findById(id)
                .orElseThrow(() -> new TicketReservationNotFoundException("Ticket Reservation not found"));

        if (TicketReservationStatus.CONFIRMED.equals(ticketReservation.getStatus())) {
            logger.error(TICKET_RESERVATION_CONFIRM, "Ticket Reservation status is CONFIRMED already");
            throw new InvalidTicketReservationException("Ticket Reservation status is CONFIRMED already");
        }

        ticketReservation.setStatus(TicketReservationStatus.CONFIRMED);
        ticketReservation.setConfirmedAt(OffsetDateTime.now());

        return ticketReservationsRepository.save(ticketReservation);
    }

    @Transactional
    public TicketReservation releaseTicketReservation(UUID id) {
        logger.info(TICKET_RESERVATION_RELEASE, "releaseTicketReservation method entered");
        TicketReservation ticketReservation = ticketReservationsRepository.findById(id)
                .orElseThrow(() -> new TicketReservationNotFoundException("Ticket Reservation not found"));

        if (TicketReservationStatus.RELEASED.equals(ticketReservation.getStatus())) {
            logger.error(TICKET_RESERVATION_RELEASE, "Ticket Reservation status is RELEASED already");
            throw new InvalidTicketReservationException("Ticket Reservation status is RELEASED already");
        }

        ticketReservation.setStatus(TicketReservationStatus.RELEASED);
        ticketReservation.setReleasedAt(OffsetDateTime.now());

        createStockMovement(ticketReservation, StockMovementType.IN);

        return ticketReservationsRepository.save(ticketReservation);
    }

    public TicketReservation getTicketReservation(UUID reservationId) {
        logger.info(TICKET_RESERVATION_GET, "getTicketReservation method entered");
        return ticketReservationsRepository
                .findById(reservationId)
                .orElseThrow(() -> new TicketReservationNotFoundException("Ticket Reservation not found"));
    }

    public boolean eventHasReservations(UUID eventId) {
        return ticketReservationsRepository.existsByEventId(eventId);
    }

    public boolean sessionHasReservations(UUID sessionId) {
        return ticketReservationsRepository.existsBySessionId(sessionId);
    }

    public boolean tierHasReservations(UUID tierId) {
        return ticketReservationsRepository.existsByTierId((tierId));
    }


    private void createStockMovement(TicketReservation ticketReservation, StockMovementType stockMovementType) {
        StockMovement stockMovement = new StockMovement();
        TicketStockId ticketStockId = new TicketStockId(ticketReservation.getEventId(), ticketReservation.getSessionId(), ticketReservation.getTierId());
        TicketStock stock = ticketStocksRepository.findById(ticketStockId)
                .orElseThrow(() -> new TicketStockNotFoundException("Ticket Reservation not found"));
        stockMovement.setTicketStock(stock);
        stockMovement.setQuantity(ticketReservation.getQuantity());
        stockMovement.setType(stockMovementType);
        ticketStocksService.addStockMovement(stockMovement);
    }

}

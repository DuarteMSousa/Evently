package org.example.services;

import org.example.enums.StockMovementType;
import org.example.enums.TicketReservationStatus;
import org.example.exceptions.TicketReservationNotFoundException;
import org.example.exceptions.TicketStockNotFoundException;
import org.example.models.StockMovement;
import org.example.models.TicketReservation;
import org.example.models.TicketStock;
import org.example.models.TicketStockId;
import org.example.repositories.TicketReservationsRepository;
import org.example.repositories.TicketStocksRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
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

    private ModelMapper modelMapper = new ModelMapper();

    @Transactional
    public TicketReservation createTicketReservation(TicketReservation ticketReservation) {

        ticketReservation.setStatus(TicketReservationStatus.HELD);
        ticketReservation.setExpiresAt(null);
        ticketReservation.setReleasedAt(null);
        ticketReservation.setConfirmedAt(null);

        createStockMovement(ticketReservation,StockMovementType.OUT);

        return ticketReservationsRepository.save(ticketReservation);
    }

    @Transactional
    public TicketReservation confirmTicketReservation(UUID id) {
        TicketReservation ticketReservation = ticketReservationsRepository.findById(id)
                .orElseThrow(() -> new TicketReservationNotFoundException("Ticket Reservation not found"));


        ticketReservation.setStatus(TicketReservationStatus.CONFIRMED);
        ticketReservation.setConfirmedAt(OffsetDateTime.now());

        return ticketReservationsRepository.save(ticketReservation);
    }

    @Transactional
    public TicketReservation releaseTicketReservation(UUID id) {
        TicketReservation ticketReservation = ticketReservationsRepository.findById(id)
                .orElseThrow(() -> new TicketReservationNotFoundException("Ticket Reservation not found"));

        ticketReservation.setStatus(TicketReservationStatus.RELEASED);
        ticketReservation.setReleasedAt(OffsetDateTime.now());

        createStockMovement(ticketReservation,StockMovementType.IN);

        return ticketReservationsRepository.save(ticketReservation);
    }

    public TicketReservation getTicketReservation(UUID reservationId) {
        return ticketReservationsRepository
                .findById(reservationId)
                .orElseThrow(() -> new TicketReservationNotFoundException("Ticket Reservation not found"));
    }

    private void createStockMovement(TicketReservation ticketReservation, StockMovementType stockMovementType) {
        StockMovement stockMovement = new StockMovement();
        TicketStockId ticketStockId = new TicketStockId(ticketReservation.getEventId(),ticketReservation.getSessionId(),ticketReservation.getTierId());
        TicketStock stock= ticketStocksRepository.findById(ticketStockId)
                .orElseThrow(() -> new TicketStockNotFoundException("Ticket Reservation not found"));
        stockMovement.setTicketStock(stock);
        stockMovement.setQuantity(ticketReservation.getQuantity());
        stockMovement.setType(stockMovementType);
        ticketStocksService.addStockMovement(stockMovement);
    }


}

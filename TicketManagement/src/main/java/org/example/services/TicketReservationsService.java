package org.example.services;

import jakarta.transaction.Transactional;
import org.example.enums.StockMovementType;
import org.example.enums.TicketReservationStatus;
import org.example.exceptions.InvalidTicketReservationException;
import org.example.exceptions.TicketReservationNotFoundException;
import org.example.exceptions.TicketStockNotFoundException;
import org.example.messages.received.OrderPaidMessage;
import org.example.messages.received.RefundRequestDecisionRegisteredMessage;
import org.example.models.StockMovement;
import org.example.models.TicketReservation;
import org.example.models.TicketStock;
import org.example.models.TicketStockId;
import org.example.publishers.TicketManagementMessagesPublisher;
import org.example.repositories.TicketReservationsRepository;
import org.example.repositories.TicketStocksRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TicketReservationsService {

    @Autowired
    private TicketManagementMessagesPublisher ticketManagementMessagesPublisher;

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

    /**
     * Creates a new ticket reservation.
     * <p>
     * The reservation is initially created with status {@link TicketReservationStatus#HELD}.
     * It validates the requested quantity and registers an outgoing stock movement.
     *
     * @param ticketReservation the ticket reservation data to be created
     * @return the persisted ticket reservation
     * @throws InvalidTicketReservationException if the quantity is less than or equal to zero
     * @throws TicketStockNotFoundException      if no stock exists for the given event, session, and tier
     */
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

    /**
     * Confirms an existing ticket reservation.
     * <p>
     * Updates the reservation status to {@link TicketReservationStatus#CONFIRMED}
     * and records the confirmation timestamp.
     *
     * @param id the unique identifier of the ticket reservation
     * @return the confirmed ticket reservation
     * @throws TicketReservationNotFoundException if the reservation does not exist
     * @throws InvalidTicketReservationException  if the reservation is already confirmed
     */
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

    /**
     * Releases an existing ticket reservation.
     * <p>
     * Updates the reservation status to {@link TicketReservationStatus#RELEASED},
     * records the release timestamp, and returns the reserved tickets to stock.
     *
     * @param id the unique identifier of the ticket reservation
     * @return the released ticket reservation
     * @throws TicketReservationNotFoundException if the reservation does not exist
     * @throws InvalidTicketReservationException  if the reservation is already released
     */
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

    /**
     * Retrieves a ticket reservation by its unique identifier.
     *
     * @param reservationId the unique identifier of the ticket reservation
     * @return the ticket reservation
     * @throws TicketReservationNotFoundException if the reservation does not exist
     */
    public TicketReservation getTicketReservation(UUID reservationId) {
        logger.info(TICKET_RESERVATION_GET, "getTicketReservation method entered");
        return ticketReservationsRepository
                .findById(reservationId)
                .orElseThrow(() -> new TicketReservationNotFoundException("Ticket Reservation not found"));
    }

    /**
     * Checks whether an event has any ticket reservations.
     * <p>
     *
     * @param eventId the unique identifier of the event
     * @return true if the event has at least one reservation, false otherwise
     */
    public boolean eventHasReservations(UUID eventId) {
        return ticketReservationsRepository.existsByEventId(eventId);
    }

    /**
     * Checks whether a session has any ticket reservations.
     * <p>
     *
     * @param sessionId the unique identifier of the session
     * @return true if the session has at least one reservation, false otherwise
     */
    public boolean sessionHasReservations(UUID sessionId) {
        return ticketReservationsRepository.existsBySessionId(sessionId);
    }

    /**
     * Checks whether a tier has any ticket reservations.
     * <p>
     *
     * @param tierId the unique identifier of the tier
     * @return true if the tier has at least one reservation, false otherwise
     */
    public boolean tierHasReservations(UUID tierId) {
        return ticketReservationsRepository.existsByTierId((tierId));
    }

    /**
     * Creates a stock movement associated with a ticket reservation.
     * <p>
     * Depending on the {@link StockMovementType}, the stock will be increased or decreased
     * according to the reservation quantity.
     *
     * @param ticketReservation the ticket reservation related to the stock movement
     * @param stockMovementType the type of stock movement (IN or OUT)
     * @throws TicketStockNotFoundException if the ticket stock does not exist
     */
    private void createStockMovement(TicketReservation ticketReservation, StockMovementType stockMovementType) {
        StockMovement stockMovement = new StockMovement();
        TicketStockId ticketStockId = new TicketStockId(ticketReservation.getEventId(), ticketReservation.getSessionId(), ticketReservation.getTierId());
        TicketStock stock = ticketStocksRepository.findById(ticketStockId)
                .orElseThrow(() -> new TicketStockNotFoundException("Ticket Stock not found"));
        stockMovement.setTicketStock(stock);
        stockMovement.setQuantity(ticketReservation.getQuantity());
        stockMovement.setType(stockMovementType);
        ticketStocksService.addStockMovement(stockMovement);
    }

    @Transactional
    public void handleOrderPaid(OrderPaidMessage orderPaidMessage) {

        List<TicketReservation> ticketReservations = ticketReservationsRepository.findByOrderId(orderPaidMessage.getId());

        ticketReservations.forEach(ticketReservation -> {
            ticketReservation.setConfirmedAt(OffsetDateTime.now());
            ticketReservation.setStatus(TicketReservationStatus.CONFIRMED);
            ticketReservation.setConfirmedAt(OffsetDateTime.now());
            ticketReservationsRepository.save(ticketReservation);
        });

        ticketReservations.forEach(ticketReservation -> {
            ticketManagementMessagesPublisher.publishTicketReservationConfirmedMessage(ticketReservation);
        });
    }

    @Transactional
    public void handleRefundRequestDecision(RefundRequestDecisionRegisteredMessage message) {


//        ticketReservations.forEach(ticketReservation -> {
//            ticketManagementMessagesPublisher.publishTicketReservationConfirmedMessage(ticketReservation);
//        });
    }

}

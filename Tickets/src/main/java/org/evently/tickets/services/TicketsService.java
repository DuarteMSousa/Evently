package org.evently.tickets.services;

import jakarta.transaction.Transactional;
import org.evently.tickets.config.MQConfig;
import org.evently.tickets.enums.TicketStatus;
import org.evently.tickets.exceptions.InvalidTicketUpdateException;
import org.evently.tickets.exceptions.TicketNotFoundException;
import org.evently.tickets.messages.TicketIssuedMessage;
import org.evently.tickets.models.Ticket;
import org.evently.tickets.repositories.TicketsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class TicketsService {

    private static final Logger logger = LoggerFactory.getLogger(TicketsService.class);

    private static final Marker TICKET_CREATE = MarkerFactory.getMarker("TICKET_CREATE");
    private static final Marker TICKET_GET = MarkerFactory.getMarker("TICKET_GET");
    private static final Marker TICKET_CANCEL = MarkerFactory.getMarker("TICKET_CANCEL");
    private static final Marker TICKET_USE = MarkerFactory.getMarker("TICKET_USE");
    private static final Marker TICKET_VALIDATION = MarkerFactory.getMarker("TICKET_VALIDATION");

    @Autowired
    private TicketsRepository ticketsRepository;

    @Autowired
    private RabbitTemplate template;

    /**
     * Retrieves a ticket by its unique identifier.
     *
     * @param id ticket identifier
     * @return found ticket
     * @throws TicketNotFoundException if the ticket does not exist
     */
    public Ticket getTicket(UUID id) {
        logger.debug(TICKET_GET, "Get ticket requested (id={})", id);

        return ticketsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(TICKET_GET, "Ticket not found (id={})", id);
                    return new TicketNotFoundException("Ticket not found");
                });
    }

    /**
     * Issues a new ticket after validating all required fields.
     *
     * @param ticket ticket to be issued
     * @return persisted ticket
     * @throws InvalidTicketUpdateException if the ticket data is invalid
     */
    @Transactional
    public Ticket issueTicket(Ticket ticket) {
        logger.info(TICKET_CREATE, "Registering new ticket (userId={}, eventId={}, tierId={})",
                ticket.getUserId(), ticket.getEventId(), ticket.getTierId());

        validateTicket(ticket);

        Ticket savedTicket = ticketsRepository.save(ticket);

        logger.info(TICKET_CREATE, "Ticket registered successfully (id={}, status={})",
                savedTicket.getId(), savedTicket.getStatus());

        TicketIssuedMessage ticketIssuedMessage = new TicketIssuedMessage();
        ticketIssuedMessage.setId(savedTicket.getId());
        ticketIssuedMessage.setReservationId(savedTicket.getReservationId());
        ticketIssuedMessage.setOrderId(savedTicket.getOrderId());
        ticketIssuedMessage.setUserId(savedTicket.getUserId());
        ticketIssuedMessage.setEventId(savedTicket.getEventId());
        ticketIssuedMessage.setSessionId(savedTicket.getSessionId());
        ticketIssuedMessage.setTierId(savedTicket.getTierId());

        template.convertAndSend(MQConfig.EXCHANGE, MQConfig.ROUTING_KEY, ticketIssuedMessage);

        return savedTicket;
    }

    /**
     * Cancels an existing ticket, as long as it has not been used or already cancelled.
     *
     * @param id ticket identifier
     * @return cancelled ticket
     * @throws TicketNotFoundException if the ticket does not exist
     * @throws InvalidTicketUpdateException if the ticket is already used or cancelled
     */
    @Transactional
    public Ticket cancelTicket(UUID id) {
        logger.info(TICKET_CANCEL, "Cancelling ticket (id={})", id);

        Ticket ticket = getTicket(id);

        if (ticket.getStatus() == TicketStatus.USED){
            throw new InvalidTicketUpdateException("Ticket is already used");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new InvalidTicketUpdateException("Ticket is already cancelled");
        }

        ticket.setStatus(TicketStatus.CANCELLED);

        Ticket cancelledTicket = ticketsRepository.save(ticket);

        logger.info(TICKET_CANCEL, "Ticket cancelled successfully (id={})", id);

        return cancelledTicket;
    }

    /**
     * Validates and marks a ticket as used.
     *
     * @param id ticket identifier
     * @return updated ticket marked as used
     * @throws TicketNotFoundException if the ticket does not exist
     * @throws InvalidTicketUpdateException if the ticket is already used or cancelled
     */
    @Transactional
    public Ticket useTicket(UUID id) {
        logger.info(TICKET_USE, "Validating/Using ticket (id={})", id);

        Ticket ticket = getTicket(id);

        if (ticket.getStatus() == TicketStatus.USED){
            throw new InvalidTicketUpdateException("Ticket is already used");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new InvalidTicketUpdateException("Ticket is already cancelled");
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setValidatedAt(new Date());

        Ticket updatedTicket = ticketsRepository.save(ticket);
        logger.info(TICKET_USE, "Ticket used successfully (id={})", id);

        return updatedTicket;
    }

    /**
     * Retrieves a paginated list of tickets associated with a user.
     *
     * @param userId user identifier
     * @param pageNumber page number (1-based)
     * @param pageSize page size
     * @return page of user tickets
     */
    public Page<Ticket> getTicketsByUser(UUID userId, Integer pageNumber, Integer pageSize) {
        if (pageSize > 50 || pageSize < 1) {
            pageSize = 50;
        }

        if (pageNumber < 1) {
            pageNumber = 1;
        }

        logger.debug(TICKET_GET, "Fetching tickets for user (userId={}, page={}, size={})",
                userId, pageNumber, pageSize);

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return ticketsRepository.findAllByUserId(userId, pageable);
    }

    /**
     * Validates all required ticket fields before issuing the ticket.
     *
     * @param ticket ticket to validate
     * @throws InvalidTicketUpdateException if any required field is missing
     */
    private void validateTicket(Ticket ticket) {
        logger.debug(TICKET_VALIDATION, "Validating ticket payload (userId={}, eventId={})",
                ticket.getUserId(), ticket.getEventId());

        if (ticket.getReservationId() == null) {
            logger.warn(TICKET_VALIDATION, "Missing reservationId");
            throw new InvalidTicketUpdateException("Reservation ID is required");
        }
        if (ticket.getOrderId() == null) {
            logger.warn(TICKET_VALIDATION, "Missing orderId");
            throw new InvalidTicketUpdateException("Order ID is required");
        }
        if (ticket.getUserId() == null) {
            logger.warn(TICKET_VALIDATION, "Missing userId");
            throw new InvalidTicketUpdateException("User ID is required");
        }
        if (ticket.getEventId() == null) {
            logger.warn(TICKET_VALIDATION, "Missing eventId");
            throw new InvalidTicketUpdateException("Event ID is required");
        }
        if (ticket.getSessionId() == null) {
            logger.warn(TICKET_VALIDATION, "Missing sessionId");
            throw new InvalidTicketUpdateException("Session ID is required");
        }
        if (ticket.getTierId() == null) {
            logger.warn(TICKET_VALIDATION, "Missing tier id");
            throw new InvalidTicketUpdateException("Tier ID is required");
        }
        if (ticket.getStatus() == null) {
            logger.warn(TICKET_VALIDATION, "Missing status");
            throw new InvalidTicketUpdateException("Ticket status is required");
        }
    }
}
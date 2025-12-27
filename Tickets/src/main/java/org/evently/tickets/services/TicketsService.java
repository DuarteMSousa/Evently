package org.evently.tickets.services;

import jakarta.transaction.Transactional;
import org.evently.tickets.enums.TicketStatus;
import org.evently.tickets.exceptions.InvalidTicketUpdateException;
import org.evently.tickets.exceptions.TicketNotFoundException;
import org.evently.tickets.models.Ticket;
import org.evently.tickets.repositories.TicketsRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
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

    public Ticket getTicket(UUID id) {
        logger.debug(TICKET_GET, "Get ticket requested (id={})", id);

        return ticketsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(TICKET_GET, "Ticket not found (id={})", id);
                    return new TicketNotFoundException("Ticket not found");
                });
    }

    @Transactional
    public Ticket issueTicket(Ticket ticket) {
        logger.info(TICKET_CREATE, "Registering new ticket (userId={}, eventId={}, tierId={})",
                ticket.getUserId(), ticket.getEventId(), ticket.getTierId());

        validateTicket(ticket);

        Ticket savedTicket = ticketsRepository.save(ticket);

        logger.info(TICKET_CREATE, "Ticket registered successfully (id={}, status={})",
                savedTicket.getId(), savedTicket.getStatus());

        return savedTicket;
    }

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
}
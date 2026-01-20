package org.example.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.example.clients.TicketManagementClient;
import org.example.clients.VenuesClient;
import org.example.dtos.externalServices.venues.VenueDTO;
import org.example.enums.EventStatus;
import org.example.exceptions.*;
import org.example.models.Event;
import org.example.models.EventSession;
import org.example.repositories.EventSessionsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventSessionsService {

    @Autowired
    private EventSessionsRepository eventSessionsRepository;

    @Autowired
    private EventsService eventsService;

    @Autowired
    private TicketManagementClient ticketManagementClient;

    private Logger logger = LoggerFactory.getLogger(CategoriesService.class);

    private static final Marker SESSION_GET = MarkerFactory.getMarker("SESSION_GET");
    private static final Marker SESSION_DELETE = MarkerFactory.getMarker("SESSION_DELETE");
    private static final Marker SESSION_UPDATE = MarkerFactory.getMarker("SESSION_UPDATE");
    private static final Marker SESSION_CREATE = MarkerFactory.getMarker("SESSION_CREATE");

    @Autowired
    private VenuesClient venuesClient;

    /**
     * Creates a new event session.
     *
     * @param eventSession session payload (must include event, venueId, startsAt, endsAt)
     * @return persisted session
     * @throws InvalidEventSessionException if session payload is invalid (dates/event/venue)
     * @throws ExternalServiceException     if VenuesService fails unexpectedly (FeignException)
     */
    @Transactional
    public EventSession createEventSession(EventSession eventSession) {
        logger.info(SESSION_CREATE, "createEventSession method entered");

        validateEventSession(eventSession, SESSION_CREATE);

        return eventSessionsRepository.save(eventSession);
    }

    /**
     * Updates an existing event session.
     *
     * @param id           event session identifier from the request path
     * @param eventSession updated session payload (must include id)
     * @return updated persisted session
     * @throws InvalidEventSessionUpdateException if path id and body id do not match
     * @throws EventSessionNotFoundException      if the session does not exist
     * @throws InvalidEventSessionException       if session payload is invalid (dates/event/venue)
     * @throws ExternalServiceException           if VenuesService fails unexpectedly (FeignException)
     */
    @Transactional
    public EventSession updateEventSession(UUID id, EventSession eventSession) {
        logger.info(SESSION_UPDATE, "updateEventSession method entered");

        if (!id.equals(eventSession.getId())) {
            logger.error(SESSION_UPDATE, "Parameter id and body id do not correspond");
            throw new InvalidEventSessionUpdateException("Parameter id and body id do not correspond");
        }

        EventSession existingEventSession = eventSessionsRepository.findById(id)
                .orElse(null);

        if (existingEventSession == null) {
            logger.error(SESSION_UPDATE, "Event Session not found");
            throw new EventSessionNotFoundException("Event Session not found");
        }

        existingEventSession.setStartsAt(eventSession.getStartsAt());
        existingEventSession.setEndsAt(eventSession.getEndsAt());

        validateEventSession(existingEventSession, SESSION_UPDATE);

        return eventSessionsRepository.save(existingEventSession);
    }

    /**
     * Retrieves an event session by its identifier.
     *
     * @param sessionId session identifier
     * @return found event session
     * @throws EventSessionNotFoundException if the session does not exist
     */
    public EventSession getEventSession(UUID sessionId) {
        logger.info(SESSION_GET, "getEventSession method entered");

        EventSession eventSession = eventSessionsRepository
                .findById(sessionId)
                .orElse(null);

        if (eventSession == null) {
            logger.error(SESSION_GET, "Event Session not found");
            throw new EventSessionNotFoundException("Event Session not found");
        }

        return eventSession;
    }

    /**
     * Deletes an event session.
     *
     * @param id session identifier
     * @throws EventSessionNotFoundException      if the session does not exist
     * @throws ExternalServiceException           if Ticket Management service fails (FeignException)
     * @throws InvalidEventSessionUpdateException if the session already has reservations
     */
    public void deleteEventSession(UUID id) {
        logger.info(SESSION_DELETE, "deleteEventSession method entered");

        EventSession eventSessionToDelete = eventSessionsRepository.findById(id).orElse(null);

        if (eventSessionToDelete == null) {
            logger.error(SESSION_DELETE, "Event Session not found");
            throw new EventSessionNotFoundException("Event Session not found");
        }

        Boolean hasReservations;

        try {
            hasReservations = ticketManagementClient.checkSessionReservations(id).getBody();
        } catch (FeignException e) {
            String errorBody = e.contentUTF8();
            logger.error(SESSION_DELETE, "FeignException while checking session reservations through ticket management service: {}", errorBody);
            throw new ExternalServiceException("Error while checking session reservations through ticket management service");
        }

        if (hasReservations.booleanValue()) {
            logger.error(SESSION_DELETE, "Session already has reservations");
            throw new InvalidEventSessionUpdateException("Session already has reservations");
        }

        Event event = eventSessionToDelete.getEvent();

        if (event.getStatus().equals(EventStatus.PUBLISHED)) {
            try {
                ticketManagementClient.deleteSessionTicketStock(eventSessionToDelete.getId());
            } catch (FeignException e) {
                String errorBody = e.contentUTF8();
                logger.error(SESSION_DELETE, "Error while trying to delete stock through ticket management service: {}", errorBody);
                throw new ExternalServiceException("Error while trying to delete stock through ticket management service");
            }
        }

        eventSessionsRepository.delete(eventSessionToDelete);
    }

    /**
     * Validates an event session payload.
     *
     * @param eventSession event session to validate
     * @param marker       log marker to use for consistent logging per operation (create/update)
     * @throws InvalidEventSessionException if session is invalid or references non-existing event/venue
     * @throws ExternalServiceException     if VenuesService fails unexpectedly (FeignException)
     */
    private void validateEventSession(EventSession eventSession, Marker marker) {
        if (eventSession.getStartsAt() == null || eventSession.getEndsAt() == null) {
            logger.error(marker, "Invalid start or end time");
            throw new InvalidEventSessionException("Invalid start or end time");
        }

        if (eventSession.getEndsAt().isBefore(eventSession.getStartsAt())) {
            logger.error(marker, "End time is before start time");
            throw new InvalidEventSessionException("End time is before start time");
        }

        List<EventSession> existingSessions = eventSessionsRepository.findSessionsByVenueAndInterval(eventSession.getVenueId(), eventSession.getStartsAt(), eventSession.getEndsAt());

        if (existingSessions.stream().anyMatch(s -> !s.getId().equals(eventSession.getId()))) {
            logger.error(marker, "There is already existing event session at the same venue at the same time");
            throw new InvalidEventSessionException("There is already existing event session at the same venue at the same time");
        }

        Event event;

        try {
            event = eventsService.getEvent(eventSession.getEvent().getId());
        } catch (EventNotFoundException e) {
            logger.error(marker, "Event not found");
            throw new InvalidEventSessionException("Event not found");
        }

        VenueDTO venue;

        try {
            venue = venuesClient.getVenue(eventSession.getVenueId()).getBody();
        } catch (FeignException.NotFound e) {
            String errorBody = e.contentUTF8();
            logger.error(marker, "Not found response while getting venue from VenuesService: {}", errorBody);
            throw new InvalidEventSessionException("Venue not found");
        } catch (FeignException e) {
            String errorBody = e.contentUTF8();
            logger.error(marker, "FeignException while getting venue from VenuesService: {}", errorBody);
            throw new ExternalServiceException("Error while getting venue from VenuesService");
        }

        if (venue == null) {
            logger.error(marker, "Venue not found");
            throw new InvalidEventSessionException("Venue not found");
        }
    }

}

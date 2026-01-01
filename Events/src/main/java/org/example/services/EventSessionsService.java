package org.example.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.example.clients.TicketReservationsClient;
import org.example.clients.VenuesClient;
import org.example.dtos.externalServices.venues.VenueDTO;
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

import java.util.UUID;

@Service
public class EventSessionsService {

    @Autowired
    private EventSessionsRepository eventSessionsRepository;

    @Autowired
    private EventsService eventsService;

    @Autowired
    private TicketReservationsClient ticketReservationsClient;

    private Logger logger = LoggerFactory.getLogger(CategoriesService.class);

    private static final Marker SESSION_GET = MarkerFactory.getMarker("SESSION_GET");

    private static final Marker SESSION_DELETE = MarkerFactory.getMarker("SESSION_DELETE");

    private static final Marker SESSION_UPDATE = MarkerFactory.getMarker("SESSION_UPDATE");

    private static final Marker SESSION_CREATE = MarkerFactory.getMarker("SESSION_CREATE");

    @Autowired
    private VenuesClient venuesClient;

    @Transactional
    public EventSession createEventSession(EventSession eventSession) {

        logger.info(SESSION_CREATE, "createEventSession method entered");

        validateEventSession(eventSession, SESSION_CREATE);

        return eventSessionsRepository.save(eventSession);
    }

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

        validateEventSession(eventSession, SESSION_UPDATE);

        existingEventSession.setEvent(eventSession.getEvent());
        existingEventSession.setVenueId(eventSession.getVenueId());
        existingEventSession.setStartsAt(eventSession.getStartsAt());
        existingEventSession.setEndsAt(eventSession.getEndsAt());

        return eventSessionsRepository.save(existingEventSession);
    }

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

    public void deleteEventSession(UUID id) {
        logger.info(SESSION_DELETE, "deleteEventSession method entered");
        EventSession eventSessionToDelete = eventSessionsRepository.findById(id).orElse(null);

        if (eventSessionToDelete == null) {
            logger.error(SESSION_DELETE, "Event Session not found");
            throw new EventSessionNotFoundException("Event Session not found");
        }
        Boolean hasReservations;

        try {
            hasReservations = ticketReservationsClient.checkSessionReservations(id).getBody();
        } catch (FeignException e) {
            String errorBody = e.contentUTF8();
            logger.error(SESSION_DELETE, "FeignException while checking session reservations through ticket management service: {}", errorBody);
            throw new ExternalServiceException("Error while checking session reservations through ticket management service");
        }

        if (hasReservations.booleanValue()) {
            logger.error(SESSION_DELETE, "Session already has reservations");
            throw new InvalidEventSessionUpdateException("Session already has reservations");
        }


        eventSessionsRepository.delete(eventSessionToDelete);
    }


    private void validateEventSession(EventSession eventSession, Marker marker) {
        if (eventSession.getStartsAt() == null || eventSession.getEndsAt() == null) {
            logger.error(marker, "Invalid start or end time");
            throw new InvalidEventSessionException("Invalid start or end time");
        }

        if (eventSession.getEndsAt().isBefore(eventSession.getStartsAt())) {
            logger.error(marker, "End time is before start time");
            throw new InvalidEventSessionException("End time is before start time");
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

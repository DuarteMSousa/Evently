package org.example.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.example.clients.TicketReservationsClient;
import org.example.clients.VenuesClient;
import org.example.dtos.externalServices.venueszone.VenueZoneDTO;
import org.example.exceptions.*;
import org.example.models.EventSession;
import org.example.models.SessionTier;
import org.example.repositories.SessionTiersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SessionTiersService {

    @Autowired
    private SessionTiersRepository sessionTiersRepository;

    @Autowired
    private EventSessionsService eventSessionsService;

    @Autowired
    private VenuesClient venuesClient;

    @Autowired
    private TicketReservationsClient ticketReservationsClient;

    private Logger logger = LoggerFactory.getLogger(EventsService.class);

    private static final Marker TIER_GET = MarkerFactory.getMarker("TIER_GET");
    private static final Marker TIER_DELETE = MarkerFactory.getMarker("TIER_DELETE");
    private static final Marker TIER_UPDATE = MarkerFactory.getMarker("TIER_UPDATE");
    private static final Marker TIER_CREATE = MarkerFactory.getMarker("TIER_CREATE");

    /**
     * Creates a new session tier.
     *
     *
     * @param sessionTier tier payload
     * @return persisted tier
     *
     * @throws SessionTierAlreadyExistsException if a tier already exists for (eventSession, zoneId)
     * @throws InvalidSessionTierException       if payload is invalid (price/session/zone mismatch)
     * @throws ExternalServiceException          if VenuesService fails unexpectedly (FeignException)
     */
    @Transactional
    public SessionTier createSessionTier(SessionTier sessionTier) {
        logger.info(TIER_CREATE, "createSessionTier method entered");

        if (sessionTiersRepository.existsByEventSessionAndZoneId(sessionTier.getEventSession(), sessionTier.getZoneId())) {
            logger.error(TIER_CREATE, "Session tier already exists");
            throw new SessionTierAlreadyExistsException("Session tier already exists");
        }

        validateSessionTier(sessionTier, TIER_CREATE);

        return sessionTiersRepository.save(sessionTier);
    }

    /**
     * Updates an existing session tier.
     *
     *
     * @param id          session tier identifier from the request path
     * @param sessionTier updated tier payload (must include id)
     * @return updated persisted tier
     *
     * @throws InvalidSessionTierUpdateException if path id and body id do not match
     * @throws SessionTierNotFoundException      if the tier does not exist
     * @throws InvalidSessionTierException       if payload is invalid
     * @throws ExternalServiceException          if VenuesService fails unexpectedly (FeignException)
     */
    @Transactional
    public SessionTier updateSessionTier(UUID id, SessionTier sessionTier) {
        logger.info(TIER_UPDATE, "updateSessionTier method entered");

        if (!id.equals(sessionTier.getId())) {
            logger.error(TIER_UPDATE, "Parameter id and body id do not correspond");
            throw new InvalidSessionTierUpdateException("Parameter id and body id do not correspond");
        }

        SessionTier existingSessionTier = sessionTiersRepository.findById(id)
                .orElse(null);

        if (existingSessionTier == null) {
            logger.error(TIER_UPDATE, "Session tier not found");
            throw new SessionTierNotFoundException("Session tier not found");
        }

        validateSessionTier(sessionTier, TIER_UPDATE);

        return sessionTiersRepository.save(existingSessionTier);
    }

    /**
     * Retrieves a session tier by its identifier.
     *
     * @param sessionTierId tier identifier
     * @return found tier
     *
     * @throws SessionTierNotFoundException if the tier does not exist
     */
    public SessionTier getSessionTier(UUID sessionTierId) {
        logger.info(TIER_GET, "getSessionTier method entered");

        SessionTier sessionTier = sessionTiersRepository
                .findById(sessionTierId)
                .orElse(null);

        if (sessionTier == null) {
            logger.error(TIER_GET, "Session tier not found");
            throw new SessionTierNotFoundException("Session tier not found");
        }

        return sessionTier;
    }

    /**
     * Deletes a session tier.
     *
     *
     * @param id tier identifier
     *
     * @throws SessionTierNotFoundException      if the tier does not exist
     * @throws ExternalServiceException          if Ticket Management service fails (FeignException)
     * @throws InvalidEventSessionUpdateException if the tier has reservations and cannot be deleted
     */
    public void deleteSessionTier(UUID id) {
        logger.info(TIER_DELETE, "deleteSessionTier method entered");

        SessionTier sessionTierToDelete = sessionTiersRepository.findById(id).orElse(null);

        if (sessionTierToDelete == null) {
            logger.error(TIER_DELETE, "Session tier not found");
            throw new SessionTierNotFoundException("Session tier not found");
        }

        Boolean hasReservations;

        try {
            hasReservations = ticketReservationsClient.checkTierReservations(id).getBody();
        } catch (FeignException e) {
            String errorBody = e.contentUTF8();
            logger.error(TIER_DELETE, "FeignException while checking tier reservations through ticket management service: {}", errorBody);
            throw new ExternalServiceException("Error while checking tier reservations through ticket management service");
        }

        if (hasReservations.booleanValue()) {
            logger.error(TIER_DELETE, "Tier already has reservations");
            throw new InvalidEventSessionUpdateException("Tier already has reservations");
        }

        sessionTiersRepository.delete(sessionTierToDelete);
    }

    /**
     * Validates a session tier payload.
     *
     *
     * @param sessionTier tier to validate
     * @param marker      log marker to use for consistent logging per operation (create/update)
     *
     * @throws InvalidSessionTierException if tier is invalid (price/session/zone mismatch or missing references)
     * @throws ExternalServiceException    if VenuesService fails unexpectedly (FeignException)
     */
    public void validateSessionTier(SessionTier sessionTier, Marker marker) {
        if (sessionTier.getPrice() <= 0) {
            logger.error(marker, "Session tier price must be greater than 0");
            throw new InvalidSessionTierException("Session tier price must be greater than 0");
        }

        EventSession eventSession;
        try {
            eventSession = eventSessionsService.getEventSession(sessionTier.getEventSession().getId());
        } catch (EventSessionNotFoundException e) {
            logger.error(marker, "Event session not found");
            throw new InvalidSessionTierException("Event session not found");
        }

        VenueZoneDTO zone;

        try {
            zone = venuesClient.getZone(sessionTier.getZoneId()).getBody();
        } catch (FeignException.NotFound e) {
            String errorBody = e.contentUTF8();
            logger.error(marker, "Not found response while getting venue zone from VenuesService: {}", errorBody);
            throw new InvalidSessionTierException("Venue zone not found");
        } catch (FeignException e) {
            String errorBody = e.contentUTF8();
            logger.error(marker, "FeignException while getting venue zone from VenuesService: {}", errorBody);
            throw new ExternalServiceException("Error while getting venue zone from VenuesService");
        }

        if (zone == null) {
            logger.error(marker, "Zone not found");
            throw new InvalidSessionTierException("Zone not found");
        }

        if (!zone.getVenueId().equals(eventSession.getVenueId())) {
            logger.error(marker, "Invalid zone, does not correspond to venue");
            throw new InvalidSessionTierException("Invalid zone, does not correspond to venue");
        }
    }
}

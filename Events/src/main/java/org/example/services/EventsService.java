package org.example.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.example.clients.OrganizationsClient;
import org.example.clients.TicketManagementClient;
import org.example.clients.VenuesClient;
import org.example.dtos.externalServices.organizations.OrganizationDTO;
import org.example.dtos.externalServices.ticketStocks.TicketStockCreateDTO;
import org.example.dtos.externalServices.ticketStocks.TicketStockIdDTO;
import org.example.dtos.externalServices.venueszone.VenueZoneDTO;
import org.example.enums.EventStatus;
import org.example.exceptions.*;
import org.example.models.Event;
import org.example.models.EventSession;
import org.example.models.SessionTier;
import org.example.publishers.EventMessagesPublisher;
import org.example.repositories.EventSessionsRepository;
import org.example.repositories.EventsRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventsService {

    @Autowired
    private EventsRepository eventsRepository;

    @Autowired
    private EventSessionsRepository eventSessionsRepository;

    @Autowired
    private OrganizationsClient organizationsClient;

    @Autowired
    private TicketManagementClient ticketManagementClient;

    @Autowired
    private VenuesClient venuesClient;

    @Autowired
    private EventMessagesPublisher  eventMessagesPublisher;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(EventsService.class);

    private static final Marker EVENTS_PAGE_GET = MarkerFactory.getMarker("EVENTS_PAGE_GET");
    private static final Marker EVENT_GET = MarkerFactory.getMarker("EVENT_GET");
    private static final Marker EVENT_UPDATE = MarkerFactory.getMarker("EVENT_UPDATE");
    private static final Marker EVENT_CREATE = MarkerFactory.getMarker("EVENT_CREATE");
    private static final Marker EVENT_CANCEL = MarkerFactory.getMarker("EVENT_CANCEL");
    private static final Marker EVENT_PUBLISH = MarkerFactory.getMarker("EVENT_PUBLISH");
    private static final Marker STOCK_GENERATED = MarkerFactory.getMarker("STOCK_GENERATED");
    private static final Marker STOCK_FAILED = MarkerFactory.getMarker("STOCK_FAILED");

    /**
     * Creates a new event.
     *
     * @param event event payload
     * @return persisted event
     * @throws EventAlreadyExistsException if an event with the same name already exists
     * @throws InvalidEventException       if payload is invalid (name/description/org validation)
     * @throws ExternalServiceException    if OrganizationsService fails (FeignException)
     */
    @Transactional
    public Event createEvent(Event event) {
        logger.info(EVENT_CREATE, "createEvent method entered");

        if (eventsRepository.existsByName(event.getName())) {
            logger.error(EVENT_CREATE, "Event with name {} already exists", event.getName());
            throw new EventAlreadyExistsException("Event with name " + event.getName() + " already exists");
        }

        validateEvent(event, EVENT_CREATE);

        event.setStatus(EventStatus.DRAFT);

        return eventsRepository.save(event);
    }

    /**
     * Updates an existing event while preserving its current status.
     *
     * @param id    event identifier from the request path
     * @param event updated event payload (must include id)
     * @return updated persisted event
     * @throws InvalidEventUpdateException if path id and body id do not match
     * @throws EventNotFoundException      if the event does not exist
     * @throws InvalidEventException       if payload is invalid (name/description/org validation)
     * @throws ExternalServiceException    if OrganizationsService fails (FeignException)
     */
    @Transactional
    public Event updateEvent(UUID id, Event event) {
        logger.info(EVENT_UPDATE, "updateEvent method entered");

        if (!id.equals(event.getId())) {
            logger.error(EVENT_UPDATE, "Parameter id and body id do not correspond");
            throw new InvalidEventUpdateException("Parameter id and body id do not correspond");
        }

        Event existingEvent = eventsRepository.findById(id)
                .orElse(null);

        if (existingEvent == null) {
            logger.error(EVENT_UPDATE, "Event not found");
            throw new EventNotFoundException("Event not found");
        }

        existingEvent.setSessions(eventSessionsRepository.findSessionsWithTiersByEventId(event.getId()));

        EventStatus status = existingEvent.getStatus();

        existingEvent.setName(event.getName());
        existingEvent.setDescription(event.getDescription());
        existingEvent.setCategories(event.getCategories());

        existingEvent.setStatus(status);

        validateEvent(existingEvent, EVENT_UPDATE);

        return eventsRepository.save(existingEvent);
    }

    /**
     * Retrieves an event by its identifier.
     *
     * @param eventId event identifier
     * @return found event
     * @throws EventNotFoundException if the event does not exist
     */
    public Event getEvent(UUID eventId) {
        logger.info(EVENT_GET, "getEvent method entered");

        Event event = eventsRepository
                .findById(eventId)
                .orElse(null);

        if (event == null) {
            logger.error(EVENT_GET, "Event not found");
            throw new EventNotFoundException("Event not found");
        }

        return event;
    }

    /**
     * Cancels an event.
     *
     * @param eventId event identifier
     * @return canceled event (persisted)
     * @throws EventNotFoundException      if the event does not exist
     * @throws EventNotPublishedException  if the event is already canceled
     * @throws InvalidEventUpdateException if the event has reservations and cannot be canceled
     * @throws ExternalServiceException    if Ticket Management service fails (FeignException)
     */
    @Transactional
    public Event cancelEvent(UUID eventId) {
        logger.info(EVENT_CANCEL, "cancelEvent method entered");

        Event eventToCancel = eventsRepository.findById(eventId)
                .orElse(null);

        if (eventToCancel == null) {
            logger.error(EVENT_CANCEL, "Event not found");
            throw new EventNotFoundException("Event not found");
        }

        if (!eventToCancel.getStatus().equals(EventStatus.PUBLISHED) && !eventToCancel.getStatus().equals(EventStatus.PENDING_STOCK_GENERATION)) {
            throw new EventNotPublishedException("Event is not published");
        }

        Boolean hasReservations;

        try {
            hasReservations = ticketManagementClient.checkEventReservations(eventId).getBody();
        } catch (FeignException e) {
            String errorBody = e.contentUTF8();
            logger.error(EVENT_CANCEL, "FeignException while checking event reservations through ticket management service: {}", errorBody);
            throw new ExternalServiceException("Error while checking event reservations through ticket management service");
        }

        if (hasReservations.booleanValue()) {
            logger.error(EVENT_CANCEL, "Event already has reservations");
            throw new InvalidEventUpdateException("Event already has reservations");
        }

        eventToCancel.setStatus(EventStatus.CANCELED);

        try {
            ticketManagementClient.deleteEventTicketStock(eventToCancel.getId());
        } catch (FeignException e) {
            String errorBody = e.contentUTF8();
            logger.error(EVENT_CANCEL, "Error while trying to delete stock through ticket management service: {}", errorBody);
            throw new ExternalServiceException("Error while trying to delete stock through ticket management service");
        }

        return eventsRepository.save(eventToCancel);
    }

    /**
     * Publishes an event by transitioning it to a "pending stock generation" state and emitting a message.
     *
     * @param eventId event identifier
     * @return updated event (persisted)
     * @throws EventNotFoundException         if the event does not exist
     * @throws EventAlreadyPublishedException if event is already published or pending stock generation
     */
    @Transactional
    public Event publishEvent(UUID eventId) {
        logger.info(EVENT_PUBLISH, "publishEvent method entered");

        Event event = eventsRepository.findById(eventId)
                .orElse(null);

        if (event == null) {
            logger.error(EVENT_PUBLISH, "Event not found");
            throw new EventNotFoundException("Event not found");
        }

        if (event.getStatus().equals(EventStatus.PUBLISHED)
                || event.getStatus().equals(EventStatus.PENDING_STOCK_GENERATION)) {
            logger.error(EVENT_PUBLISH, "Event already Published");
            throw new EventAlreadyPublishedException("Event already Published");
        }

        event.setStatus(EventStatus.PENDING_STOCK_GENERATION);

        Event savedEvent = eventsRepository.save(event);

        eventMessagesPublisher.publishEventPublishedMessage(savedEvent);

        return savedEvent;
    }

    /**
     * Handles the successful generation of stock for an event.
     * <p>
     * This method retrieves the event by its ID. If found, it transitions the
     * event status to {@code PUBLISHED} to indicate it is live and ready for
     * ticket sales.
     *
     * @param eventId The unique identifier of the event.
     * @return The updated and persisted Event entity.
     * @throws EventNotFoundException if no event exists with the provided ID.
     */
    @Transactional
    public Event handleEventStockGeneratedMessage(UUID eventId) {
        logger.info(STOCK_GENERATED,"Handling stock generated success message for Event ID: {}", eventId);

        Event event = eventsRepository.findById(eventId)
                .orElse(null);

        if (event == null) {
            logger.error(STOCK_GENERATED,"Failed to publish event. Event not found for ID: {}", eventId);
            throw new EventNotFoundException("Event not found");
        }

        event.setStatus(EventStatus.PUBLISHED);
        Event savedEvent = eventsRepository.save(event);

        logger.info(STOCK_GENERATED,"Event {} successfully updated to status PUBLISHED", eventId);

        return savedEvent;
    }

    /**
     * Handles the failure of stock generation for an event.
     * <p>
     * If the stock generation process fails, this method reverts the event status
     * back to {@code DRAFT} so that the errors can be addressed before attempting
     * to publish again.
     *
     * @param eventId The unique identifier of the event.
     * @return The updated and persisted Event entity.
     * @throws EventNotFoundException if no event exists with the provided ID.
     */
    @Transactional
    public Event handleEventStockGenerationFailedMessage(UUID eventId) {
        logger.info(STOCK_FAILED,"Handling stock generation failure message for Event ID: {}", eventId);

        Event event = eventsRepository.findById(eventId)
                .orElse(null);

        if (event == null) {
            logger.error(STOCK_FAILED,"Failed to revert event status. Event not found for ID: {}", eventId);
            throw new EventNotFoundException("Event not found");
        }

        event.setStatus(EventStatus.DRAFT);
        Event savedEvent = eventsRepository.save(event);

        logger.info(STOCK_FAILED,"Event {} status reverted to DRAFT due to stock generation failure", eventId);

        return savedEvent;
    }

    /**
     * Retrieves a paginated list of published events.
     *
     * @param pageNumber page index (as provided by the caller)
     * @param pageSize   number of results per page (clamped to 50)
     * @return a {@link Page} of events with status {@link EventStatus#PUBLISHED}
     */
    public Page<Event> getEventPage(Integer pageNumber, Integer pageSize) {
        logger.info(EVENTS_PAGE_GET, "getEventPage method entered");

        if (pageSize > 50 || pageSize < 1) {
            pageSize = 50;
        }

        if (pageNumber < 0) {
            pageNumber = 0;
        }

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return eventsRepository.findAllByStatus(EventStatus.PUBLISHED, pageable);
    }

    /**
     * Validates an event payload.
     *
     * @param event  event to validate
     * @param marker log marker to use for consistent logging per operation (create/update)
     * @throws InvalidEventException    if event data is invalid or user not in organization
     * @throws ExternalServiceException if OrganizationsService fails (FeignException)
     */
    private void validateEvent(Event event, Marker marker) {
        if (event.getName() == null || event.getName().isEmpty()) {
            logger.error(marker, "Empty event name");
            throw new InvalidEventException("Empty event name");
        }

        if (event.getDescription() == null || event.getDescription().isEmpty()) {
            logger.error(marker, "Empty description");
            throw new InvalidEventException("Empty description");
        }

        List<OrganizationDTO> userOrganizations;

        try {
            userOrganizations = organizationsClient.getOrganizationsByUser(event.getCreatedBy()).getBody();
        } catch (FeignException e) {
            String errorBody = e.contentUTF8();
            logger.error(marker, "FeignException while getting organizations by user from OrganizationsService: {}", errorBody);
            throw new ExternalServiceException("Error while getting organizations by user from OrganizationsService");
        }

        if (!userOrganizations.stream().anyMatch(org -> org.getId().equals(event.getOrganizationId()))) {
            logger.error(marker, "Organization not found or the user does not belong to this organization");
            throw new InvalidEventException("Organization not found or the user does not belong to this organization");
        }
    }
}

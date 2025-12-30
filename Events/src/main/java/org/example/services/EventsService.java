package org.example.services;

import jakarta.transaction.Transactional;
import jdk.vm.ci.code.site.Mark;
import org.example.clients.OrganizationsClient;
import org.example.dtos.externalServices.organizations.OrganizationDTO;
import org.example.enums.EventStatus;
import org.example.exceptions.*;
import org.example.models.Event;
import org.example.repositories.EventsRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


import java.util.UUID;

@Service
public class EventsService {

    @Autowired
    private EventsRepository eventsRepository;

    @Autowired
    private OrganizationsClient organizationsClient;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(EventsService.class);

    private static final Marker EVENTS_PAGE_GET = MarkerFactory.getMarker("EVENTS_GET");
    private static final Marker EVENT_GET = MarkerFactory.getMarker("EVENT_GET");
    private static final Marker EVENT_DELETE = MarkerFactory.getMarker("EVENT_DELETE");
    private static final Marker EVENT_UPDATE = MarkerFactory.getMarker("EVENT_UPDATE");
    private static final Marker EVENT_CREATE = MarkerFactory.getMarker("EVENT_CREATE");
    private static final Marker EVENT_CANCEL = MarkerFactory.getMarker("EVENT_CANCEL");
    private static final Marker EVENT_PUBLISH = MarkerFactory.getMarker("EVENT_PUBLISH");

    @Transactional
    public Event createEvent(Event event) {
        logger.info(EVENT_CREATE, "createEvent method entered");

        if (eventsRepository.existsByName(event.getName())) {
            logger.error(EVENT_CREATE, "Event with name {} already exists", event.getName());
            throw new EventAlreadyExistsException("Event with name " + event.getName() + " already exists");
        }

        validateEvent(event,EVENT_CREATE);

        event.setStatus(EventStatus.DRAFT);

        return eventsRepository.save(event);
    }

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

        validateEvent(event,EVENT_UPDATE);

        EventStatus status = existingEvent.getStatus();

        modelMapper.map(event, existingEvent);

        existingEvent.setStatus(status);

        return eventsRepository.save(existingEvent);
    }

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


    @Transactional
    public Event cancelEvent(UUID eventId) {
        Event eventToCancel = eventsRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(""));

        if (eventToCancel.getStatus().equals(EventStatus.CANCELED)) {
            throw new EventAlreadyCanceledException("Event already cancelled");
        }

        //enviar mensagem
        eventToCancel.setStatus(EventStatus.CANCELED);
        return eventsRepository.save(eventToCancel);
    }

    @Transactional
    public Event publishEvent(UUID eventId) {
        Event event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(""));

        if (event.getStatus().equals(EventStatus.PUBLISHED) || event.getStatus().equals(EventStatus.PENDING_STOCK_GENERATION)) {
            throw new EventAlreadyPublishedException("Event already cancelled");
        }

        event.setStatus(EventStatus.PENDING_STOCK_GENERATION);

        //enviar mensagem
        return eventsRepository.save(event);
    }

    public Page<Event> getEventPage(Integer pageNumber, Integer pageSize) {
        if (pageSize > 50 || pageSize < 1) {
            pageSize = 50;
        }

        if (pageNumber < 1) {
            pageNumber = 1;
        }

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return eventsRepository.findAllByStatus(EventStatus.PUBLISHED, pageable);
    }


    private void validateEvent(Event event, Marker marker) {
        if (event.getName() == null || event.getName().isEmpty()) {
            logger.error(marker, "Empty category name");
            throw new InvalidEventException("Empty category name");
        }

        if (event.getDescription() == null || event.getDescription().isEmpty()) {
            logger.error(marker, "Empty description");
            throw new InvalidEventException("Empty description");
        }

        OrganizationDTO organization = organizationsClient.getOrganization(event.getOrganizationId()).getBody();

        if (organization == null) {
            logger.error(marker, "Organization not found");
            throw new InvalidEventException("Organization with not found");
        }
    }
}

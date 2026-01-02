package org.example.controllers;

import org.example.dtos.events.EventCreateDTO;
import org.example.dtos.events.EventDTO;
import org.example.dtos.events.EventUpdateDTO;
import org.example.exceptions.*;
import org.example.models.Event;
import org.example.services.EventsService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventsController {


    @Autowired
    private EventsService eventsService;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(EventsController.class);

    private static final Marker EVENTS_PAGE_GET = MarkerFactory.getMarker("EVENTS_GET");
    private static final Marker EVENT_GET = MarkerFactory.getMarker("EVENT_GET");
    private static final Marker EVENT_UPDATE = MarkerFactory.getMarker("EVENT_UPDATE");
    private static final Marker EVENT_CREATE = MarkerFactory.getMarker("EVENT_CREATE");
    private static final Marker EVENT_CANCEL = MarkerFactory.getMarker("EVENT_CANCEL");
    private static final Marker EVENT_PUBLISH = MarkerFactory.getMarker("EVENT_PUBLISH");

    @GetMapping("/get-events-page/{pageNumber}/{pageSize}")
    public ResponseEntity<?> searchEvents(@PathVariable("pageNumber") Integer pageNumber, @PathVariable("pageSize") Integer pageSize) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 500 INTERNAL_SERVER_ERROR - undefined error
         */
        logger.info(EVENTS_PAGE_GET, "Method searchEvents entered");
        Page<EventDTO> eventsPage;

        try {
            eventsPage = eventsService.getEventPage(pageNumber, pageSize).map(event -> modelMapper.map(event, EventDTO.class));
        } catch (Exception e) {
            logger.error(EVENTS_PAGE_GET, "Exception caught while getting events: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(EVENTS_PAGE_GET, "200 OK returned, events found");
        return ResponseEntity.status(HttpStatus.OK).body(eventsPage);
    }

    @GetMapping("/get-event/{id}")
    public ResponseEntity<?> getEvent(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 404 NOT_FOUND - Event not found.
         * 500 INTERNAL_SERVER_ERROR - undefined error
         */
        logger.info(EVENT_GET, "Method getEvent entered");
        EventDTO event = null;

        try {
            event =modelMapper.map(eventsService.getEvent(id),EventDTO.class) ;
        } catch (EventNotFoundException e) {
            logger.error(EVENT_GET, "EventNotFoundException caught while getting event: {}", e.getMessage());
        } catch (Exception e) {
            logger.error(EVENT_GET, "Exception caught while getting event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(EVENT_GET, "200 OK returned, event found");
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }

    @PostMapping("/create-event/")
    public ResponseEntity<?> createEvent(@RequestBody EventCreateDTO createDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event creation.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(EVENT_CREATE, "Method createEvent entered");
        Event eventToCreate = modelMapper.map(createDTO, Event.class);
        EventDTO event = null;

        try {
            event = modelMapper.map(eventsService.createEvent(eventToCreate), EventDTO.class);
        } catch (EventAlreadyExistsException e) {
            logger.error(EVENT_CREATE, "EventAlreadyExistsException caught while creating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidEventException e) {
            logger.error(EVENT_CREATE, "InvalidEventException caught while creating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(EVENT_CREATE, "Exception caught while creating Event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(EVENT_CREATE, "200 OK returned, event created");
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }

    @PutMapping("/update-event/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable("id") UUID id, @RequestBody EventUpdateDTO updateDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event update.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(EVENT_UPDATE, "Method updateEvent entered");
        Event eventToUpdate = modelMapper.map(updateDTO, Event.class);
        EventDTO event = null;

        try {
            event = modelMapper.map(eventsService.updateEvent(id, eventToUpdate), EventDTO.class);
        } catch (InvalidEventUpdateException e) {
            logger.error(EVENT_UPDATE, "InvalidEventUpdateException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (EventAlreadyExistsException e) {
            logger.error(EVENT_UPDATE, "EventAlreadyExistsException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidEventException e) {
            logger.error(EVENT_UPDATE, "InvalidEventException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(EVENT_UPDATE, "Exception caught while updating Event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(EVENT_UPDATE, "200 OK returned, event updated");
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }

    @PutMapping("/cancel-event/{id}")
    public ResponseEntity<?> cancelEvent(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event cancellation.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(EVENT_CANCEL, "Method cancelEvent entered");
        EventDTO event = null;

        try {
            event = modelMapper.map(eventsService.cancelEvent(id), EventDTO.class);
        } catch (EventAlreadyCanceledException e) {
            logger.error(EVENT_CANCEL, "EventAlreadyCanceledException caught while canceling Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (EventNotFoundException e) {
            logger.error(EVENT_CANCEL, "EventNotFoundException caught while canceling Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(EVENT_CANCEL, "Exception caught while cancelling Event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(EVENT_CANCEL, "200 OK returned, event cancelled");
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }


    @PutMapping("/publish-event/{id}")
    public ResponseEntity<?> publishEvent(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event publish.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(EVENT_PUBLISH, "Method publishEvent entered");
        EventDTO event = null;

        try {
            event = modelMapper.map(eventsService.publishEvent(id), EventDTO.class);
        } catch (EventAlreadyPublishedException e) {
            logger.error(EVENT_PUBLISH, "EventAlreadyPublishedException caught while canceling Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(EVENT_PUBLISH, "Exception caught while publishing Event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(EVENT_PUBLISH, "200 OK returned, event published");
        return ResponseEntity.status(HttpStatus.OK).body(event);
    }
}

package org.example.controllers;

import org.example.dtos.eventSessions.EventSessionCreateDTO;
import org.example.dtos.eventSessions.EventSessionDTO;
import org.example.dtos.eventSessions.EventSessionUpdateDTO;
import org.example.exceptions.*;
import org.example.models.Event;
import org.example.models.EventSession;
import org.example.services.EventSessionsService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/events/sessions")
public class EventSessionsController {

    @Autowired
    private EventSessionsService eventSessionsService;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(EventSessionsController.class);

    private static final Marker SESSION_GET = MarkerFactory.getMarker("SESSION_GET");
    private static final Marker SESSION_DELETE = MarkerFactory.getMarker("SESSION_DELETE");
    private static final Marker SESSION_UPDATE = MarkerFactory.getMarker("SESSION_UPDATE");
    private static final Marker SESSION_CREATE = MarkerFactory.getMarker("SESSION_CREATE");

    @PostMapping("/create-event-session/")
    public ResponseEntity<?> createEventSession(@RequestBody EventSessionCreateDTO createDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event creation.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(SESSION_CREATE, "Method createEventSession entered");
        EventSession eventSessionToCreate = new EventSession();
        Event event = new Event();
        event.setId(createDTO.getEventId());
        EventSessionDTO eventSession = null;

        try {
            eventSessionToCreate.setEvent(event);
            eventSessionToCreate.setVenueId(createDTO.getVenueId());
            eventSessionToCreate.setStartsAt(createDTO.getStartsAt());
            eventSessionToCreate.setEndsAt(createDTO.getEndsAt());
            eventSession = modelMapper.map(eventSessionsService.createEventSession(eventSessionToCreate), EventSessionDTO.class);
        } catch (EventSessionAlreadyExistsException e) {
            logger.error(SESSION_CREATE, "EventSessionAlreadyExistsException caught while creating Event session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidEventSessionException e) {
            logger.error(SESSION_CREATE, "InvalidEventSessionException caught while creating Event session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(SESSION_CREATE, "Exception caught while creating Event session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(SESSION_CREATE, "200 OK returned, event session created");
        return ResponseEntity.status(HttpStatus.OK).body(eventSession);
    }

    @PutMapping("/update-event-session/{id}")
    public ResponseEntity<?> updateEventSession(@PathVariable("id") UUID id, @RequestBody EventSessionUpdateDTO updateDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event update.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(SESSION_UPDATE, "Method updateEventSession entered");
        EventSession eventSessionToUpdate = new EventSession();
        EventSessionDTO eventSession = null;

        try {
            eventSessionToUpdate.setId(updateDTO.getId());
            eventSessionToUpdate.setStartsAt(updateDTO.getStartsAt());
            eventSessionToUpdate.setEndsAt(updateDTO.getEndsAt());
            eventSession = modelMapper.map(eventSessionsService.updateEventSession(id, eventSessionToUpdate), EventSessionDTO.class);
        } catch (InvalidEventSessionUpdateException e) {
            logger.error(SESSION_UPDATE, "InvalidEventSessionUpdateException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (EventSessionAlreadyExistsException e) {
            logger.error(SESSION_UPDATE, "EventSessionAlreadyExistsException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidEventSessionException e) {
            logger.error(SESSION_UPDATE, "InvalidEventSessionException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(SESSION_UPDATE, "Exception caught while updating Event session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(SESSION_UPDATE, "200 OK returned, event session updated");
        return ResponseEntity.status(HttpStatus.OK).body(eventSession);
    }

    @DeleteMapping("/delete-event-session/{id}")
    public ResponseEntity<?> deleteEventSession(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event update.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(SESSION_DELETE, "Method deleteEventSession entered");

        try {
            eventSessionsService.deleteEventSession(id);
        } catch (InvalidEventUpdateException e) {
            logger.error(SESSION_DELETE, "InvalidEventUpdateException caught while updating event session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (EventAlreadyExistsException e) {
            logger.error(SESSION_DELETE, "EventAlreadyExistsException caught while updating event session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidEventException e) {
            logger.error(SESSION_DELETE, "InvalidEventException caught while updating event session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(SESSION_DELETE, "Exception caught while updating event session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(SESSION_DELETE, "200 OK returned, event session deleted");
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/get-event-session/{id}")
    public ResponseEntity<?> getEventSession(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 404 NOT_FOUND - Event not found.
         * 500 INTERNAL_SERVER_ERROR - undefined error
         */
        logger.info(SESSION_GET, "Method getEventSession entered");
        EventSessionDTO eventSession = null;

        try {
            eventSession =modelMapper.map(eventSessionsService.getEventSession(id),EventSessionDTO.class) ;
        } catch (EventSessionNotFoundException e) {
            logger.error(SESSION_GET, "EventSessionNotFoundException caught while getting event session: {}", e.getMessage());
        } catch (Exception e) {
            logger.error(SESSION_GET, "Exception caught while getting event session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(SESSION_GET, "200 OK returned, event session found");
        return ResponseEntity.status(HttpStatus.OK).body(eventSession);
    }

}

package org.example.controllers;

import org.example.dtos.eventSessions.EventSessionCreateDTO;
import org.example.dtos.eventSessions.EventSessionDTO;
import org.example.dtos.eventSessions.EventSessionUpdateDTO;
import org.example.dtos.events.EventCreateDTO;
import org.example.dtos.events.EventDTO;
import org.example.dtos.events.EventUpdateDTO;
import org.example.exceptions.*;
import org.example.models.Event;
import org.example.models.EventSession;
import org.example.services.EventSessionsService;
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
@RequestMapping("/events/sessions")
public class EventSessionsController {

    @Autowired
    private EventSessionsService eventSessionsService;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(EventSessionsController.class);

    private Marker marker = MarkerFactory.getMarker("EventsController");

    @PostMapping("/create-event-session/")
    public ResponseEntity<?> createEventSession(EventSessionCreateDTO createDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event creation.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(marker, "Method createEventSession entered");
        EventSession eventSessionToCreate = modelMapper.map(createDTO, EventSession.class);
        EventSessionDTO eventSession = null;

        try {
            eventSession = modelMapper.map(eventSessionsService.createEventSession(eventSessionToCreate), EventSessionDTO.class);
        } catch (EventSessionAlreadyExistsException e) {
            logger.error(marker, "EventSessionAlreadyExistsException caught while creating Event session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidEventSessionException e) {
            logger.error(marker, "InvalidEventSessionException caught while creating Event session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while creating Event session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, event session created");
        return ResponseEntity.status(HttpStatus.OK).body(eventSession);
    }

    @PutMapping("/update-event-session/{id}")
    public ResponseEntity<?> updateEventSession(@PathVariable("id") UUID id, EventSessionUpdateDTO updateDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event update.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(marker, "Method updateEventSession entered");
        EventSession eventSessionToUpdate = modelMapper.map(updateDTO, EventSession.class);
        EventSessionDTO eventSession = null;

        try {
            eventSession = modelMapper.map(eventSessionsService.updateEventSession(id, eventSessionToUpdate), EventSessionDTO.class);
        } catch (InvalidEventSessionUpdateException e) {
            logger.error(marker, "InvalidEventSessionUpdateException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (EventSessionAlreadyExistsException e) {
            logger.error(marker, "EventSessionAlreadyExistsException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidEventSessionException e) {
            logger.error(marker, "InvalidEventSessionException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while updating Event session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, event session updated");
        return ResponseEntity.status(HttpStatus.OK).body(eventSession);
    }

    @DeleteMapping("/delete-event-session/{id}")
    public ResponseEntity<?> deleteEventSession(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event update.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */

        try {
            eventSessionsService.deleteEventSession(id);
        } catch (InvalidEventUpdateException e) {
            logger.error(marker, "InvalidEventUpdateException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (EventAlreadyExistsException e) {
            logger.error(marker, "EventAlreadyExistsException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidEventException e) {
            logger.error(marker, "InvalidEventException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while updating Event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, event updated");
        return ResponseEntity.status(HttpStatus.OK).build();
    }


    @GetMapping("/get-event-session/{id}")
    public ResponseEntity<?> getEventSession(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 404 NOT_FOUND - Event not found.
         * 500 INTERNAL_SERVER_ERROR - undefined error
         */
        logger.info(marker, "Method getEventSession entered");
        EventSessionDTO eventSession = null;

        try {
            eventSession =modelMapper.map(eventSessionsService.getEventSession(id),EventSessionDTO.class) ;
        } catch (EventSessionNotFoundException e) {
            logger.error(marker, "EventSessionNotFoundException caught while getting event session: {}", e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while getting event session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, event session found");
        return ResponseEntity.status(HttpStatus.OK).body(eventSession);
    }
}

package org.example.controllers;

import org.example.dtos.eventSessions.EventSessionDTO;
import org.example.dtos.sessionTiers.SessionTierCreateDTO;
import org.example.dtos.sessionTiers.SessionTierDTO;
import org.example.dtos.sessionTiers.SessionTierUpdateDTO;
import org.example.exceptions.*;
import org.example.models.SessionTier;
import org.example.services.SessionTiersService;
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
@RequestMapping("/events/sessions/tiers")
public class SessionTiersController {

    @Autowired
    private SessionTiersService sessionTiersService;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(SessionTiersController.class);

    private static final Marker TIER_GET = MarkerFactory.getMarker("TIER_GET");
    private static final Marker TIER_DELETE = MarkerFactory.getMarker("TIER_DELETE");
    private static final Marker TIER_UPDATE = MarkerFactory.getMarker("TIER_UPDATE");
    private static final Marker TIER_CREATE = MarkerFactory.getMarker("TIER_CREATE");

    @PostMapping("/create-session-tier/")
    public ResponseEntity<?> createSessionTier(@RequestBody SessionTierCreateDTO createDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event creation.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(TIER_CREATE, "Method createSessionTier entered");
        SessionTier sessionTierToCreate = modelMapper.map(createDTO, SessionTier.class);
        SessionTierDTO sessionTier = null;

        try {
            sessionTier = modelMapper.map(sessionTiersService.createSessionTier(sessionTierToCreate), SessionTierDTO.class);
        } catch (SessionTierAlreadyExistsException e) {
            logger.error(TIER_CREATE, "SessionTierAlreadyExistsException caught while creating Event session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidSessionTierException e) {
            logger.error(TIER_CREATE, "InvalidSessionTierException caught while creating Event session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(TIER_CREATE, "Exception caught while creating Event session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(TIER_CREATE, "200 OK returned, session tier created");
        return ResponseEntity.status(HttpStatus.OK).body(sessionTier);
    }

    @PutMapping("/update-event-session/{id}")
    public ResponseEntity<?> updateSessionTier(@PathVariable("id") UUID id, @RequestBody SessionTierUpdateDTO updateDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event update.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(TIER_UPDATE, "Method updateSessionTier entered");
        SessionTier sessionTierToUpdate = modelMapper.map(updateDTO, SessionTier.class);
        SessionTierDTO sessionTier = null;

        try {
            sessionTier = modelMapper.map(sessionTiersService.updateSessionTier(id, sessionTierToUpdate), SessionTierDTO.class);
        } catch (InvalidSessionTierUpdateException e) {
            logger.error(TIER_UPDATE, "InvalidSessionTierUpdateException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (SessionTierAlreadyExistsException e) {
            logger.error(TIER_UPDATE, "SessionTierAlreadyExistsException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidSessionTierException e) {
            logger.error(TIER_UPDATE, "InvalidSessionTierException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(TIER_UPDATE, "Exception caught while updating Event session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(TIER_UPDATE, "200 OK returned, session tier updated");
        return ResponseEntity.status(HttpStatus.OK).body(sessionTier);
    }

    @DeleteMapping("/delete-event-session/{id}")
    public ResponseEntity<?> deleteSessionTier(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event update.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(TIER_DELETE, "Method deleteSessionTier entered");

        try {
            sessionTiersService.deleteSessionTier(id);
        } catch (SessionTierNotFoundException e) {
            logger.error(TIER_DELETE, "SessionTierNotFoundException caught while deleting session tier");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(TIER_DELETE, "Exception caught while deleting session tier: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(TIER_DELETE, "200 OK returned, session tier deleted");
        return ResponseEntity.status(HttpStatus.OK).build();
    }


    @GetMapping("/get-session-tier/{id}")
    public ResponseEntity<?> getSessionTier(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 404 NOT_FOUND - Event not found.
         * 500 INTERNAL_SERVER_ERROR - undefined error
         */
        logger.info(TIER_GET, "Method getSessionTier entered");
        SessionTierDTO sessionTier = null;

        try {
            sessionTier =modelMapper.map(sessionTiersService.getSessionTier(id),SessionTierDTO.class) ;
        } catch (SessionTierNotFoundException e) {
            logger.error(TIER_GET, "SessionTierNotFoundException caught while getting session tier: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(TIER_GET, "Exception caught while getting session tier: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(TIER_GET, "200 OK returned, session tier found");
        return ResponseEntity.status(HttpStatus.OK).body(sessionTier);
    }
}

package org.example.controllers;

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

    private Marker marker = MarkerFactory.getMarker("EventsController");


    @PostMapping("/create-session-tier/")
    public ResponseEntity<?> createSessionTier(SessionTierCreateDTO createDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event creation.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(marker, "Method createSessionTier entered");
        SessionTier sessionTierToCreate = modelMapper.map(createDTO, SessionTier.class);
        SessionTierDTO sessionTier = null;

        try {
            sessionTier = modelMapper.map(sessionTiersService.createSessionTier(sessionTierToCreate), SessionTierDTO.class);
        } catch (SessionTierAlreadyExistsException e) {
            logger.error(marker, "SessionTierAlreadyExistsException caught while creating Event session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidSessionTierException e) {
            logger.error(marker, "InvalidSessionTierException caught while creating Event session");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while creating Event session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, session tier created");
        return ResponseEntity.status(HttpStatus.OK).body(sessionTier);
    }

    @PutMapping("/update-event-session/{id}")
    public ResponseEntity<?> updateSessionTier(@PathVariable("id") UUID id, SessionTierUpdateDTO updateDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event update.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(marker, "Method updateSessionTier entered");
        SessionTier sessionTierToUpdate = modelMapper.map(updateDTO, SessionTier.class);
        SessionTierDTO sessionTier = null;

        try {
            sessionTier = modelMapper.map(sessionTiersService.updateSessionTier(id, sessionTierToUpdate), SessionTierDTO.class);
        } catch (InvalidSessionTierUpdateException e) {
            logger.error(marker, "InvalidSessionTierUpdateException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (SessionTierAlreadyExistsException e) {
            logger.error(marker, "SessionTierAlreadyExistsException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (InvalidSessionTierException e) {
            logger.error(marker, "InvalidSessionTierException caught while updating Event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while updating Event session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, session tier updated");
        return ResponseEntity.status(HttpStatus.OK).body(sessionTier);
    }

    @DeleteMapping("/delete-event-session/{id}")
    public ResponseEntity<?> deleteSessionTier(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event update.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */

        try {
            sessionTiersService.deleteSessionTier(id);
        } catch (SessionTierNotFoundException e) {
            logger.error(marker, "SessionTierNotFoundException caught while deleting session tier");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while updating Event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, session tier deleted");
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

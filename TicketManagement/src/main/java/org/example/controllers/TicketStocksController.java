package org.example.controllers;

import org.example.models.TicketStock;
import org.example.services.TicketStocksService;
import org.example.exceptions.TicketStockAlreadyExistsException;
import org.example.exceptions.TicketStockNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ticketManagement/ticketStocks")
public class TicketStocksController {

    @Autowired
    private TicketStocksService ticketStocksService;

    private static final Logger logger = LoggerFactory.getLogger(TicketStocksController.class);

    private static final Marker TICKET_STOCK_CREATE = MarkerFactory.getMarker("TICKET_STOCK_CREATE");
    private static final Marker EVENT_TICKET_STOCK_DELETE = MarkerFactory.getMarker("EVENT_TICKET_STOCK_DELETE");
    private static final Marker SESSION_TICKET_STOCK_DELETE = MarkerFactory.getMarker("SESSION_TICKET_STOCK_DELETE");
    private static final Marker TIER_TICKET_STOCK_DELETE = MarkerFactory.getMarker("TIER_TICKET_STOCK_DELETE");


    @PostMapping("/create-stock")
    public ResponseEntity<?> createTicketStock(@RequestBody TicketStock ticketStock) {
        logger.info(TICKET_STOCK_CREATE, "createTicketStock method entered");
        try {
            TicketStock createdStock = ticketStocksService.createTicketStock(ticketStock);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdStock);
        } catch (TicketStockAlreadyExistsException e) {
            logger.error(TICKET_STOCK_CREATE, "Conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            logger.error(TICKET_STOCK_CREATE, "Internal error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    @DeleteMapping("/delete-event-stock/{eventId}")
    public ResponseEntity<?> deleteEventTicketStock(@PathVariable("eventId") UUID eventId) {
        logger.info(EVENT_TICKET_STOCK_DELETE, "deleteEventTicketStock method entered for eventId {}", eventId);
        try {
            List<TicketStock> deletedStocks = ticketStocksService.deleteEventTicketStock(eventId);
            return ResponseEntity.status(HttpStatus.OK).body(deletedStocks);
        } catch (TicketStockNotFoundException e) {
            logger.error(EVENT_TICKET_STOCK_DELETE, "Not Found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(EVENT_TICKET_STOCK_DELETE, "Internal error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    @DeleteMapping("/delete-session-stock/{sessionId}")
    public ResponseEntity<?> deleteSessionTicketStock(@PathVariable("sessionId") UUID sessionId) {
        logger.info(SESSION_TICKET_STOCK_DELETE, "deleteSessionTicketStock method entered for sessionId {}", sessionId);
        try {
            List<TicketStock> deletedStocks = ticketStocksService.deleteSessionTicketStock(sessionId);
            return ResponseEntity.status(HttpStatus.OK).body(deletedStocks);
        } catch (TicketStockNotFoundException e) {
            logger.error(SESSION_TICKET_STOCK_DELETE, "Not Found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(SESSION_TICKET_STOCK_DELETE, "Internal error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete-tier-stock/{tierId}")
    public ResponseEntity<?> deleteTierTicketStock(@PathVariable("tierId") UUID tierId) {
        logger.info(TIER_TICKET_STOCK_DELETE, "deleteTierTicketStock method entered for tierId {}", tierId);
        try {
            List<TicketStock> deletedStocks = ticketStocksService.deleteTierTicketStock(tierId);
            return ResponseEntity.status(HttpStatus.OK).body(deletedStocks);
        } catch (TicketStockNotFoundException e) {
            logger.error(TIER_TICKET_STOCK_DELETE, "Not Found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(TIER_TICKET_STOCK_DELETE, "Internal error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
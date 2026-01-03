package org.example.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.example.clients.VenuesClient;
import org.example.dtos.externalServices.eventSessions.EventSessionDTO;
import org.example.dtos.externalServices.sessionTiers.SessionTierDTO;
import org.example.dtos.externalServices.venueszone.VenueZoneDTO;
import org.example.enums.StockMovementType;
import org.example.enums.externalServices.EventStatus;
import org.example.exceptions.*;
import org.example.messages.TicketStockGeneratedMessage;
import org.example.models.StockMovement;
import org.example.models.TicketStock;
import org.example.models.TicketStockId;
import org.example.repositories.TicketStocksRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class TicketStocksService {

    @Autowired
    private TicketStocksRepository ticketStocksRepository;

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private VenuesClient venuesClient;

    private ModelMapper modelMapper = new ModelMapper();

    private static final Logger logger = LoggerFactory.getLogger(TicketStocksService.class);

    private static final Marker TICKET_STOCK_GET = MarkerFactory.getMarker("TICKET_STOCK_GET");
    private static final Marker TICKET_STOCK_CREATE = MarkerFactory.getMarker("TICKET_STOCK_CREATE");
    private static final Marker TICKET_STOCK_MOVEMENT_ADD = MarkerFactory.getMarker("TICKET_STOCK_MOVEMENT_ADD");

    /**
     * Creates a new ticket stock entry.
     * <p>
     * Initializes the available quantity to zero and persists the stock.
     *
     * @param ticketStock the ticket stock to be created
     * @return the persisted ticket stock
     * @throws TicketStockAlreadyExistsException if a stock entry with the same ID already exists
     */
    @Transactional
    public TicketStock createTicketStock(TicketStock ticketStock) {

        logger.info(TICKET_STOCK_CREATE, "createTicketStock method entered");

        if (ticketStocksRepository.existsById(ticketStock.getId())) {
            logger.error(TICKET_STOCK_CREATE, "Ticket Stock already exists");
            throw new TicketStockAlreadyExistsException("Ticket stock already exists");
        }

        ticketStock.setAvailableQuantity(0);

        return ticketStocksRepository.save(ticketStock);
    }

    /**
     * Adds a stock movement to a ticket stock.
     * <p>
     * Updates the available quantity based on the movement type
     * ({@link StockMovementType#IN} or {@link StockMovementType#OUT})
     * and persists the updated stock.
     *
     * @param movement the stock movement to be applied
     * @return the updated ticket stock
     * @throws TicketStockNotFoundException if the associated ticket stock does not exist
     * @throws InvalidStockMovementException if the resulting available quantity would be negative
     */
    @Transactional
    public TicketStock addStockMovement(StockMovement movement) {
        logger.info(TICKET_STOCK_MOVEMENT_ADD, "addStockMovement method entered");

        TicketStock ticketStock = ticketStocksRepository
                .findById(movement.getTicketStock().getId())
                .orElseThrow(() -> new TicketStockNotFoundException("Ticket stock not found"));


        ticketStock.getStockMovementList().add(movement);

        Integer addedQuantity = movement.getType().equals(StockMovementType.IN) ? movement.getQuantity() : -movement.getQuantity();

        ticketStock.setAvailableQuantity(ticketStock.getAvailableQuantity() + addedQuantity);

        if (ticketStock.getAvailableQuantity() < 0) {
            logger.error(TICKET_STOCK_MOVEMENT_ADD, "Ticket Stock cannot be negative");
            throw new InvalidStockMovementException("Ticket stock not found");
        }

        return ticketStocksRepository.save(ticketStock);
    }

    /**
     * Retrieves the ticket stock for a given event, session, and tier.
     *
     * @param eventId   the unique identifier of the event
     * @param sessionId the unique identifier of the session
     * @param tierId    the unique identifier of the tier
     * @return the corresponding ticket stock
     * @throws TicketStockNotFoundException if the ticket stock does not exist
     */
    @Transactional
    public TicketStock getTicketStock(UUID eventId, UUID sessionId, UUID tierId) {
        logger.error(TICKET_STOCK_GET, "getTicketStock method entered");
        TicketStockId ticketStockId = new TicketStockId(eventId, sessionId, tierId);

        TicketStock stock = ticketStocksRepository.findById(ticketStockId).orElse(null);

        if (stock == null) {
            logger.error(TICKET_STOCK_GET, "Ticket Stock not found");
            throw new TicketStockNotFoundException("Ticket stock not found");
        }

        return stock;

    }

    /**
     * Handles an event update message and manages ticket stock accordingly.
     * <p>
     * For events with status {@link EventStatus#PENDING_STOCK_GENERATION},
     * creates ticket stock entries for all sessions and tiers that do not yet have stock.
     * <p>
     * For events with status {@link EventStatus#CANCELED},
     * existing ticket stock entries can be removed or handled accordingly.
     * <p>
     * After processing, a {@link TicketStockGeneratedMessage} is published.
     *
     * @param event the event update message containing sessions, tiers, and event status
     * @throws VenueZoneNotFoundException if the venue zone does not exist
     * @throws ExternalServiceException if an error occurs while communicating with the Venues service
     */
//    @Transactional
//    public void handleEventUpdatedEvent(EventUpdatedMessage event) {
//        for (EventSessionDTO session : event.getSessions()) {
//            for (SessionTierDTO tier : session.getTiers()) {
//
//                TicketStock stock;
//
//                TicketStockId stockId = new TicketStockId(event.getId(), session.getId(), tier.getId());
//                stock = ticketStocksRepository.findById(stockId).orElse(null);
//
//                if (stock == null && event.getStatus() == EventStatus.PENDING_STOCK_GENERATION) {
//                    stock = new TicketStock();
//                    VenueZoneDTO zone;
//
//                    //ver markers
//                    try {
//                        zone = venuesClient.getZone(tier.getZoneId()).getBody();
//                    } catch (FeignException.NotFound e) {
//                        String errorBody = e.contentUTF8();
//                        logger.error( "Not found response while getting venue zone from VenuesService: {}", errorBody);
//                        throw new VenueZoneNotFoundException("Venue zone not found");
//                    } catch (FeignException e) {
//                        String errorBody = e.contentUTF8();
//                        logger.error( "FeignException while getting venue zone from VenuesService: {}", errorBody);
//                        throw new ExternalServiceException("Error while getting venue zone from VenuesService");
//                    }
//
//                    stock.setAvailableQuantity(zone.getCapacity());
//                    stock.setId(stockId);
//
//                    this.createTicketStock(stock);
//                } else if (stock != null && event.getStatus() == EventStatus.CANCELED) {
//
//                    //ticketStocksService.deleteStock(stock);
//
//                }
//
//            }
//        }
//
//        TicketStockGeneratedMessage stockGeneratedEvent = new TicketStockGeneratedMessage();
//        stockGeneratedEvent.setEventId(event.getId());
//
//        template.convertAndSend(stockGeneratedEvent);
//    }

}

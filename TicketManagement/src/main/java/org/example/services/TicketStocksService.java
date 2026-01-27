package org.example.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.example.clients.VenuesClient;
import org.example.dtos.externalServices.eventSessions.EventSessionDTO;
import org.example.dtos.externalServices.sessionTiers.SessionTierDTO;
import org.example.dtos.externalServices.venues.venueszone.VenueZoneDTO;
import org.example.enums.StockMovementType;
import org.example.exceptions.*;
import org.example.models.StockMovement;
import org.example.models.TicketStock;
import org.example.models.TicketStockId;
import org.example.publishers.EventTicketManagementMessagesPublisher;
import org.example.repositories.TicketStocksRepository;
import org.example.messages.received.EventPublishedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TicketStocksService {

    @Autowired
    private TicketStocksRepository ticketStocksRepository;

    @Autowired
    private EventTicketManagementMessagesPublisher ticketManagementMessagesPublisher;

    private static final Logger logger = LoggerFactory.getLogger(TicketStocksService.class);

    private static final Marker TICKET_STOCK_GET = MarkerFactory.getMarker("TICKET_STOCK_GET");
    private static final Marker TICKET_STOCK_CREATE = MarkerFactory.getMarker("TICKET_STOCK_CREATE");
    private static final Marker TICKET_STOCK_MOVEMENT_ADD = MarkerFactory.getMarker("TICKET_STOCK_MOVEMENT_ADD");
    private static final Marker EVENT_TICKET_STOCK_DELETE = MarkerFactory.getMarker("EVENT_TICKET_STOCK_DELETE");
    private static final Marker SESSION_TICKET_STOCK_DELETE = MarkerFactory.getMarker("SESSION_TICKET_STOCK_DELETE");
    private static final Marker TIER_TICKET_STOCK_DELETE = MarkerFactory.getMarker("TIER_TICKET_STOCK_DELETE");
    private static final Marker HANDLE_EVENT_PUBLISHED = MarkerFactory.getMarker("HANDLE_EVENT_PUBLISHED");

    @Autowired
    private VenuesClient venuesClient;

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
     * @throws TicketStockNotFoundException  if the associated ticket stock does not exist
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
     * Deletes all ticket stock entries associated with a given event.
     *
     * @param eventId the unique identifier of the event
     * @return the list of deleted ticket stock entries
     * @throws TicketStockNotFoundException if no ticket stock exists for the given event
     */
    @Transactional
    public List<TicketStock> deleteEventTicketStock(UUID eventId) {
        logger.info(EVENT_TICKET_STOCK_DELETE, "deleteEventTicketStock method entered for eventId {}", eventId);

        List<TicketStock> stocks = ticketStocksRepository.findByIdEventId(eventId);

        if (stocks.isEmpty()) {
            logger.error(EVENT_TICKET_STOCK_DELETE, "Ticket Stock not found for eventId {}", eventId);
            throw new TicketStockNotFoundException("Ticket stock not found for event");
        }

        ticketStocksRepository.deleteAll(stocks);

        return stocks;
    }

    /**
     * Deletes all ticket stock entries associated with a given session.
     *
     * @param sessionId the unique identifier of the session
     * @return the list of deleted ticket stock entries
     * @throws TicketStockNotFoundException if no ticket stock exists for the given session
     */
    @Transactional
    public List<TicketStock> deleteSessionTicketStock(UUID sessionId) {
        logger.info(SESSION_TICKET_STOCK_DELETE, "deleteSessionTicketStock method entered for sessionId {}", sessionId);

        List<TicketStock> stocks = ticketStocksRepository.findByIdSessionId((sessionId));

        if (stocks.isEmpty()) {
            logger.error(SESSION_TICKET_STOCK_DELETE, "Ticket Stock not found for sessionId {}", sessionId);
            throw new TicketStockNotFoundException("Ticket stock not found for session");
        }

        ticketStocksRepository.deleteAll(stocks);

        return stocks;
    }

    /**
     * Deletes all ticket stock entries associated with a given tier.
     *
     * @param tierId the unique identifier of the tier
     * @return the list of deleted ticket stock entries
     * @throws TicketStockNotFoundException if no ticket stock exists for the given tier
     */
    @Transactional
    public List<TicketStock> deleteTierTicketStock(UUID tierId) {
        logger.info(TIER_TICKET_STOCK_DELETE, "deleteTierTicketStock method entered for tierId {}", tierId);

        List<TicketStock> stocks = ticketStocksRepository.findByIdTierId((tierId));

        if (stocks.isEmpty()) {
            logger.error(TIER_TICKET_STOCK_DELETE, "Ticket Stock not found for tierId {}", tierId);
            throw new TicketStockNotFoundException("Ticket stock not found for tier");
        }

        ticketStocksRepository.deleteAll(stocks);

        return stocks;
    }

    @Transactional
    public void handleEventPublishedMessage(EventPublishedMessage message) {
        logger.info(HANDLE_EVENT_PUBLISHED, "handleEventPublishedMessage method entered for event {}", message.getId());
        try {

            List<VenueZoneDTO> venueZoneCache = new ArrayList<VenueZoneDTO>();
            for (EventSessionDTO eventSession : message.getSessions()) {
                for (SessionTierDTO tier : eventSession.getTiers()) {
                    Optional<VenueZoneDTO> optionalZone = venueZoneCache.stream()
                            .filter(zone -> zone.getId().equals(tier.getZoneId()))
                            .findFirst();

                    VenueZoneDTO venueZoneDTO;

                    if (optionalZone.isPresent()) {
                        venueZoneDTO = optionalZone.get();
                    } else {
                        try {
                            venueZoneDTO = venuesClient.getZone(tier.getZoneId()).getBody();
                            venueZoneCache.add(venueZoneDTO);
                        } catch (FeignException e) {
                            logger.error(HANDLE_EVENT_PUBLISHED, "Cannot get venue zone", e);
                            throw new ExternalServiceException("Cannot get venue zone");
                        }
                    }

                    TicketStockId ticketStockIdDTO = new TicketStockId();
                    ticketStockIdDTO.setEventId(message.getId());
                    ticketStockIdDTO.setSessionId(eventSession.getId());
                    ticketStockIdDTO.setTierId(tier.getId());

                    TicketStock ticketStockCreate = new TicketStock();
                    ticketStockCreate.setAvailableQuantity(venueZoneDTO.getCapacity());
                    ticketStockCreate.setId(ticketStockIdDTO);

                    try {
                        this.createTicketStock(ticketStockCreate);
                    } catch (TicketStockAlreadyExistsException e) {
                        //do nothing
                    }

                }
            }

            ticketManagementMessagesPublisher.publishEventTicketStockGeneratedMessage(message.getId());

        } catch (Exception e) {
            logger.error(HANDLE_EVENT_PUBLISHED, "Error publishing event ticket stock", e);
            ticketManagementMessagesPublisher.publishEventTicketStockGenerationFailedMessage(message.getId());
        }

    }

}

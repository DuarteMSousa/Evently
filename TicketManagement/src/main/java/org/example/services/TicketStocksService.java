package org.example.services;

import jakarta.transaction.Transactional;
import org.example.clients.VenuesClient;
import org.example.dtos.externalServices.eventSessions.EventSessionDTO;
import org.example.dtos.externalServices.sessionTiers.SessionTierDTO;
import org.example.dtos.externalServices.venueszone.VenueZoneDTO;
import org.example.enums.StockMovementType;
import org.example.enums.externalServices.EventStatus;
import org.example.events.TicketStockGeneratedEvent;
import org.example.events.received.EventUpdatedEvent;
import org.example.exceptions.InvalidStockMovementException;
import org.example.exceptions.TicketStockAlreadyExistsException;
import org.example.exceptions.TicketStockNotFoundException;
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


    @Transactional
    public void handleEventUpdatedEvent(EventUpdatedEvent event) {
        for (EventSessionDTO session : event.getSessions()) {
            for (SessionTierDTO tier : session.getTiers()) {

                TicketStock stock;

                TicketStockId stockId = new TicketStockId(event.getId(), session.getId(), tier.getId());
                stock = ticketStocksRepository.findById(stockId).orElse(null);

                if (stock == null && event.getStatus() == EventStatus.PENDING_STOCK_GENERATION) {
                    stock = new TicketStock();

                    VenueZoneDTO zone = venuesClient.getZone(tier.getZoneId()).getBody();

                    if(zone ==null){
                        //throw new VenueZoneNotFoundException("Zone not found");
                    }

                    stock.setAvailableQuantity(zone.getCapacity());
                    stock.setId(stockId);

                    //try catch
                    this.createTicketStock(stock);

                } else if (stock != null && event.getStatus() == EventStatus.CANCELED) {

                    //ticketStocksService.deleteStock(stock);

                }

            }
        }

        TicketStockGeneratedEvent stockGeneratedEvent = new TicketStockGeneratedEvent();
        stockGeneratedEvent.setEventId(event.getId());

        template.convertAndSend(stockGeneratedEvent);
    }

}

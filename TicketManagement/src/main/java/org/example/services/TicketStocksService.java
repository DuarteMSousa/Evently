package org.example.services;

import jakarta.transaction.Transactional;
import org.example.enums.StockMovementType;
import org.example.exceptions.TicketStockAlreadyExistsException;
import org.example.exceptions.TicketStockNotFoundException;
import org.example.models.StockMovement;
import org.example.models.TicketStock;
import org.example.repositories.TicketStocksRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class TicketStocksService {

    @Autowired
    private TicketStocksRepository ticketStocksRepository;

    private static final Logger logger = LoggerFactory.getLogger(TicketStocksService.class);

    private static final Marker TICKET_STOCK_CREATE = MarkerFactory.getMarker("TICKET_STOCK_CREATE");
    private static final Marker TICKET_STOCK_MOVEMENT_ADD = MarkerFactory.getMarker("TICKET_STOCK_MOVEMENT_ADD");

    @Transactional
    public TicketStock createTicketStock(TicketStock ticketStock) {

        logger.info(TICKET_STOCK_CREATE,"createTicketStock method entered");

        if (ticketStocksRepository.existsById(ticketStock.getId())) {
            logger.error(TICKET_STOCK_CREATE,"Ticket Stock already exists");
            throw new TicketStockAlreadyExistsException("Ticket stock already exists");
        }

        ticketStock.setAvailableQuantity(0);

        return ticketStocksRepository.save(ticketStock);
    }

    @Transactional
    public TicketStock addStockMovement(StockMovement movement) {
        logger.info(TICKET_STOCK_MOVEMENT_ADD,"addStockMovement method entered");

        TicketStock ticketStock = ticketStocksRepository
                .findById(movement.getTicketStock().getId())
                .orElseThrow(() -> new TicketStockNotFoundException("Ticket stock not found"));


        ticketStock.getStockMovementList().add(movement);

        Integer addedQuantity = movement.getType().equals(StockMovementType.IN) ? movement.getQuantity() : -movement.getQuantity();

        ticketStock.setAvailableQuantity(ticketStock.getAvailableQuantity() + addedQuantity);

        return ticketStocksRepository.save(ticketStock);
    }


}

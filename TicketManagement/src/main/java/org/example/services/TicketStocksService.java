package org.example.services;

import org.example.enums.StockMovementType;
import org.example.exceptions.TicketStockAlreadyExistsException;
import org.example.exceptions.TicketStockNotFoundException;
import org.example.models.StockMovement;
import org.example.models.TicketStock;
import org.example.repositories.TicketStocksRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;


@Service
public class TicketStocksService {

    @Autowired
    private TicketStocksRepository ticketStocksRepository;

    private ModelMapper modelMapper = new ModelMapper();

    @Transactional
    public TicketStock createTicketStock(TicketStock ticketStock) {
        if (ticketStocksRepository.existsById(ticketStock.getId())) {
            throw new TicketStockAlreadyExistsException("Ticket stock already exists");
        }

        ticketStock.setAvailableQuantity(0);

        return ticketStocksRepository.save(ticketStock);
    }

    @Transactional
    public TicketStock addStockMovement(StockMovement movement) {
        TicketStock ticketStock = ticketStocksRepository
                .findById(movement.getTicketStock().getId())
                .orElseThrow(() -> new TicketStockNotFoundException("Ticket stock not found"));


        ticketStock.getStockMovementList().add(movement);

        Integer addedQuantity = movement.getType().equals(StockMovementType.IN) ? movement.getQuantity() : -movement.getQuantity();

        ticketStock.setAvailableQuantity(ticketStock.getAvailableQuantity() + addedQuantity);

        return ticketStocksRepository.save(ticketStock);
    }


}

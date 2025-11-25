package org.example.services;

import org.example.exceptions.TicketStockAlreadyExistsException;
import org.example.exceptions.TicketStockNotFoundException;
import org.example.models.TicketReservation;
import org.example.models.TicketStock;
import org.example.models.TicketStockId;
import org.example.repositories.TicketReservationsRepository;
import org.example.repositories.TicketStocksRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
public class TicketStocksService {

    @Autowired
    private TicketStocksRepository ticketStocksRepository;

    private ModelMapper modelMapper = new ModelMapper();

    @Transactional
    public TicketStock createTicketStock(UUID eventId, UUID sessionId, UUID tierId) {
        TicketStockId ticketStockId = new TicketStockId(eventId, sessionId, tierId);
        if (ticketStocksRepository.existsById(ticketStockId)) {
            throw new TicketStockAlreadyExistsException("Ticket stock already exists");
        }

        TicketStock ticketStock = new TicketStock();

        ticketStock.setId(ticketStockId);

        ticketStock.setAvailableQuantity(0);

        return ticketStocksRepository.save(ticketStock);
    }

    @Transactional
    public TicketStock addTicketStockQuantity(UUID eventId, UUID sessionId, UUID tierId, Integer quantity) {
        TicketStockId ticketStockId = new TicketStockId(eventId, sessionId, tierId);

        TicketStock ticketStock = ticketStocksRepository
                .findById(ticketStockId)
                .orElseThrow(() -> new TicketStockNotFoundException("Ticket stock not found"));

        ticketStock.setAvailableQuantity(ticketStock.getAvailableQuantity()+quantity);

        return ticketStocksRepository.save(ticketStock);
    }



}

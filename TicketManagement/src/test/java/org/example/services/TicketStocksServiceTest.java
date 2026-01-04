package org.example.services;

import org.example.enums.StockMovementType;
import org.example.exceptions.InvalidStockMovementException;
import org.example.exceptions.TicketStockAlreadyExistsException;
import org.example.exceptions.TicketStockNotFoundException;
import org.example.models.StockMovement;
import org.example.models.TicketStock;
import org.example.models.TicketStockId;
import org.example.repositories.TicketStocksRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketStocksServiceTest {

    @Mock private TicketStocksRepository ticketStocksRepository;
    @Mock private RabbitTemplate template;

    @InjectMocks private TicketStocksService ticketStocksService;

    private TicketStock stock;

    @BeforeEach
    void setup() {
        stock = new TicketStock();
        stock.setId(new TicketStockId(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        stock.setAvailableQuantity(10);
        if (stock.getStockMovementList() == null) {
            stock.setStockMovementList(new java.util.ArrayList<>());
        }
    }

    @Test
    void createTicketStock_alreadyExists_throws() {
        when(ticketStocksRepository.existsById(stock.getId())).thenReturn(true);

        TicketStockAlreadyExistsException ex = assertThrows(TicketStockAlreadyExistsException.class,
                () -> ticketStocksService.createTicketStock(stock));

        assertEquals("Ticket stock already exists", ex.getMessage());
        verify(ticketStocksRepository, never()).save(any());
    }

    @Test
    void createTicketStock_success_saves() {
        when(ticketStocksRepository.existsById(stock.getId())).thenReturn(false);
        when(ticketStocksRepository.save(any(TicketStock.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketStock saved = ticketStocksService.createTicketStock(stock);
        assertNotNull(saved);
        verify(ticketStocksRepository).save(stock);
    }

    @Test
    void addStockMovement_stockNotFound_throws() {
        StockMovement mv = new StockMovement();
        mv.setTicketStock(stock);
        mv.setType(StockMovementType.IN);
        mv.setQuantity(1);

        when(ticketStocksRepository.findById(stock.getId())).thenReturn(Optional.empty());

        TicketStockNotFoundException ex = assertThrows(TicketStockNotFoundException.class,
                () -> ticketStocksService.addStockMovement(mv));

        assertEquals("Ticket stock not found", ex.getMessage());
    }

    @Test
    void addStockMovement_increasesQuantity() {
        StockMovement mv = new StockMovement();
        mv.setTicketStock(stock);
        mv.setType(StockMovementType.IN);
        mv.setQuantity(5);

        when(ticketStocksRepository.findById(stock.getId())).thenReturn(Optional.of(stock));
        when(ticketStocksRepository.save(any(TicketStock.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketStock updated = ticketStocksService.addStockMovement(mv);

        assertEquals(15, updated.getAvailableQuantity());
        assertEquals(1, updated.getStockMovementList().size());
        verify(ticketStocksRepository).save(stock);
    }

    @Test
    void addStockMovement_out_negative_throwsInvalid() {
        StockMovement mv = new StockMovement();
        mv.setTicketStock(stock);
        mv.setType(StockMovementType.OUT);
        mv.setQuantity(999);

        when(ticketStocksRepository.findById(stock.getId())).thenReturn(Optional.of(stock));

        InvalidStockMovementException ex = assertThrows(InvalidStockMovementException.class,
                () -> ticketStocksService.addStockMovement(mv));

        // mensagem no código atual
        assertEquals("Ticket stock not found", ex.getMessage());
    }

    @Test
    void deleteEventTicketStock_empty_throwsNotFound() {
        UUID eventId = UUID.randomUUID();
        when(ticketStocksRepository.findByIdEventId(eventId)).thenReturn(Collections.emptyList());

        TicketStockNotFoundException ex = assertThrows(TicketStockNotFoundException.class,
                () -> ticketStocksService.deleteEventTicketStock(eventId));

        assertEquals("Ticket stock not found for event", ex.getMessage());
    }

    @Test
    void deleteEventTicketStock_success_deletesAll() {
        UUID eventId = UUID.randomUUID();
        List<TicketStock> list = java.util.Arrays.asList(stock);
        when(ticketStocksRepository.findByIdEventId(eventId)).thenReturn(list);

        List<TicketStock> deleted = ticketStocksService.deleteEventTicketStock(eventId);

        assertEquals(1, deleted.size());
        verify(ticketStocksRepository).deleteAll(list);
    }
}
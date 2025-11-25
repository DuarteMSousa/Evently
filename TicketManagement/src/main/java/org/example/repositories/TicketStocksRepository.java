package org.example.repositories;

import org.example.models.TicketReservation;
import org.example.models.TicketStock;
import org.example.models.TicketStockId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketStocksRepository extends JpaRepository<TicketStock, TicketStockId> {

}

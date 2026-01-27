package org.example.repositories;

import org.example.models.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockMovementsRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByTicketStockIdEventId(UUID ticketStockIdEventId);
}

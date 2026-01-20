package org.example.repositories;

import org.example.models.TicketStock;
import org.example.models.TicketStockId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketStocksRepository extends JpaRepository<TicketStock, TicketStockId> {

    List<TicketStock> findByIdEventId(UUID idEventId);

    List<TicketStock> findByIdTierId(UUID idTierId);

    List<TicketStock> findByIdSessionId(UUID idSessionId);

}

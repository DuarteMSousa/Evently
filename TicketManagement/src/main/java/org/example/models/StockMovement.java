package org.example.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.StockMovementType;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "event_id", referencedColumnName = "eventId",nullable = false),
            @JoinColumn(name = "session_id", referencedColumnName = "sessionId",nullable = false),
            @JoinColumn(name = "tier_id", referencedColumnName = "tierId",nullable = false),
    })
    private TicketStock ticketStock;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private StockMovementType type;

}

package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "ticket_stocks")
@EntityListeners(AuditingEntityListener.class)
public class TicketStock {

    @EmbeddedId
    private TicketStockId id;

    @Column(nullable = false)
    private Integer availableQuantity;

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;

    @OneToMany(mappedBy = "ticketStock", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<StockMovement> stockMovementList;
}


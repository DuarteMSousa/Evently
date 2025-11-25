package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "ticket_stocks")
@EntityListeners(AuditingEntityListener.class)
public class TicketStock {

    @EmbeddedId
    private TicketStockId id;

    private Integer availableQuantity;

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;
}


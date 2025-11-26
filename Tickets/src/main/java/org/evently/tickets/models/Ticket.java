package org.evently.tickets.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.evently.tickets.enums.TicketStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "tickets")
@EntityListeners(AuditingEntityListener.class)
public class Ticket {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID reservationId;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private UUID tierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TicketStatus status;

    @CreatedDate
    private Date issuedAt;

    @Column()
    private Date validatedAt;
}

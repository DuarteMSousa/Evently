package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.TicketReservationStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "ticket_reservations")
@EntityListeners(AuditingEntityListener.class)

public class TicketReservation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private UUID tierId;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private TicketReservationStatus status;

    private OffsetDateTime expiresAt;

    private OffsetDateTime releasedAt;

    private OffsetDateTime confirmedAt;

    @CreatedDate
    private Date createdAt;
}

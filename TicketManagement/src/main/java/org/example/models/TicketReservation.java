package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.TicketReservationStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "ticket_reservations")
@EntityListeners(AuditingEntityListener.class)

public class TicketReservation {

    @Id
    private UUID id;

    private UUID userId;

    private UUID tierId;

    private UUID sessionId;

    private UUID eventId;

    private Integer quantity;

    private TicketReservationStatus status;

    private OffsetDateTime expiresAt;

    private OffsetDateTime releasedAt;

    private OffsetDateTime confirmedAt;

    @CreatedDate
    private Date createdAt;
}

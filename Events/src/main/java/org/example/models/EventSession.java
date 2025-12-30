package org.example.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "event_sessions")
@EntityListeners(AuditingEntityListener.class)

public class EventSession {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eventId")
    private Event event;

    private UUID venueId;

    private Instant startsAt;

    private Instant endsAt;

    private UUID createdBy;

    @CreatedDate
    private Date createdAt;

    private UUID updatedBy;

    @LastModifiedDate
    private Date updatedAt;

    @OneToMany(mappedBy = "eventSession", cascade = CascadeType.ALL)
    private List<SessionTier> tiers;

}

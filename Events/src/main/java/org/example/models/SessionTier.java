package org.example.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "session_tiers")
@EntityListeners(AuditingEntityListener.class)

public class SessionTier {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eventSessionId")
    private EventSession eventSession;

    private UUID zoneId;

    @Column(nullable = false, scale = 2)
    private float price;

    private UUID createdBy;

    @CreatedDate
    private Date createdAt;

    private UUID updatedBy;

    @LastModifiedDate
    private Date updatedAt;

}

package org.example.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.NotificationChannel;
import org.example.enums.OutboxStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "outbox_messages")
@EntityListeners(AuditingEntityListener.class)
public class OutBoxMessage {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID notificationId;

    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(nullable = false, length = 10)
    private OutboxStatus status;

    @Column(nullable = false)
    private Integer attempts;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    private Date sentAt;
}
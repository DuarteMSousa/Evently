package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
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
    private UUID notificationId; // ligação lógica à Notification

    @Column(nullable = false, length = 10)
    private String channel; // EMAIL, PUSH, SMS

    @Column(nullable = false, length = 10)
    private String status; // PENDING, SENT, FAILED

    @Column(nullable = false)
    private Integer attempts;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    private Date sentAt;
}
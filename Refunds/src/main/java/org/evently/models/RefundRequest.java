package org.evently.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.evently.enums.RefundRequestStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "refund_requests")
@EntityListeners(AuditingEntityListener.class)
public class RefundRequest {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RefundRequestStatus status;

    @CreatedDate
    private Date createdAt;

    @Column()
    private Date decisionAt;

    @Column()
    private Date processedAt;

    @OneToMany(mappedBy = "refundRequest", cascade = CascadeType.ALL)
    private List<RefundRequestMessage> messages;

    @OneToOne(mappedBy = "refundRequest", cascade = CascadeType.ALL)
    private RefundDecision refundDecision;
}

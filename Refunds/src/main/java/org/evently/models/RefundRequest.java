package org.evently.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.evently.enums.RefundRequestStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
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
    private UUID payment;

    @Column(nullable = false)
    private UUID user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, length = 100)
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

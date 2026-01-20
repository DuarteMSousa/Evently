package org.evently.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.evently.enums.DecisionType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "refund_decisions")
@EntityListeners(AuditingEntityListener.class)
public class RefundDecision {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID decidedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DecisionType decisionType;

    @Column(length = 200)
    private String description;

    @CreatedDate
    private Date createdAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refundRequestId", nullable = false)
    private RefundRequest refundRequest;

}

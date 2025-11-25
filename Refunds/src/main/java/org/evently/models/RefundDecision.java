package org.evently.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.evently.enums.DecisionType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
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

    @Column(nullable = false, length = 100)
    private DecisionType decisionType;

    @Column(nullable = false)
    private String description;

    @CreatedDate
    private Date createdAt;

    @OneToOne(mappedBy = "review", cascade = CascadeType.ALL)
    private RefundRequest refundRequest;
}

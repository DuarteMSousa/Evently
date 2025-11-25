package org.evently.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "refund_request_message")
@EntityListeners(AuditingEntityListener.class)
public class RefundRequestMessage {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID user;

    @Column(nullable = false, length = 100)
    private String content;

    @CreatedDate
    private Date createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refundRequest", nullable = false)
    @JsonIgnore
    private RefundRequest refundRequest;
}

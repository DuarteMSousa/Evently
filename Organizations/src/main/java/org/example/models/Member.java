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
@Table(name = "members")
@EntityListeners(AuditingEntityListener.class)
public class Member {

    @EmbeddedId
    private MemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("organizationId")
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private UUID createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Date createdAt;
}
package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.EventStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "events")
@EntityListeners(AuditingEntityListener.class)
public class Event {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    private String description;

    private UUID organizationId;

    private EventStatus status;

    private UUID createdBy;

    @CreatedDate
    private Date createdAt;

    private UUID updatedBy;

    @LastModifiedDate
    private Date updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<EventSession> sessions;

    @ManyToMany()
    private List<Category> category;
}

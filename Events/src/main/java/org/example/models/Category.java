package org.example.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "categories")
@EntityListeners(AuditingEntityListener.class)

public class Category {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    private UUID createdBy;

    @CreatedDate
    private Date createdAt;

    private UUID updatedBy;

    @LastModifiedDate
    private Date updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    private List<Event> events;

}

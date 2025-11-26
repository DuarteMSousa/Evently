package org.evently.reviews.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.evently.reviews.enums.EntityType;
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
@Table(name = "reviews")
@EntityListeners(AuditingEntityListener.class)
public class Review {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID authorId;

    @Column(nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EntityType entityType;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 100)
    private String comment;

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL)
    private List<ReviewComment> comments;
}

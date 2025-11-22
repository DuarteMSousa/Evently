package org.evently.reviews.models;

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
@Table(name = "reviewComments")
@EntityListeners(AuditingEntityListener.class)
public class ReviewComment {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID authorId;

    @ManyToOne
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    private String comment;

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;

}

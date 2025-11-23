package org.evently.reviews.dtos.reviews;

import lombok.Getter;
import lombok.Setter;
import org.evently.reviews.enums.EntityType;
import org.evently.reviews.models.ReviewComment;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
public class ReviewDTO {

    private UUID id;

    private UUID author;

    private UUID entity;

    private EntityType entityType;

    private int rating;

    private String comment;

    private List<ReviewComment> comments;

    private Date createdAt;

    private Date updatedAt;

}

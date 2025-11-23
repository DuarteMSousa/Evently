package org.evently.reviews.dtos.reviewComments;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Setter
@Getter
public class ReviewCommentDTO {

    private UUID id;

    private UUID author;

    private UUID reviewId;

    private String comment;

    private Date createdAt;

    private Date updatedAt;

}

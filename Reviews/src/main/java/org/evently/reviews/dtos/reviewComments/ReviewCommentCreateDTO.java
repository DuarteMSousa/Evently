package org.evently.reviews.dtos.reviewComments;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class ReviewCommentCreateDTO {

    private UUID author;

    private UUID reviewId;

    private String comment;

}

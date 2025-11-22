package org.evently.reviews.dtos.reviewComments;

import lombok.Getter;
import lombok.Setter;
import org.evently.reviews.models.Review;

import java.util.UUID;

@Setter
@Getter
public class ReviewCommentCreateDTO {

    private UUID authorId;

    private Review review;

    private String comment;

}

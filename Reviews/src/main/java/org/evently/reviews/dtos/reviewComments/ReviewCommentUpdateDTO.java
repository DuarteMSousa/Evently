package org.evently.reviews.dtos.reviewComments;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class ReviewCommentUpdateDTO {

    private UUID id;
    private String comment;

}

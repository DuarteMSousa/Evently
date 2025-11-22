package org.evently.reviews.dtos.reviews;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReviewUpdateDTO {

    private int rating;

    private String comment;

}

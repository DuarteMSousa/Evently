package org.evently.reviews.dtos.reviews;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class ReviewUpdateDTO {

    private UUID id;

    private int rating;

    private String comment;

}

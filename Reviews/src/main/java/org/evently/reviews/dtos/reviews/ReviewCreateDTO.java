package org.evently.reviews.dtos.reviews;

import lombok.Getter;
import lombok.Setter;
import org.evently.reviews.enums.EntityType;

import java.util.UUID;

@Setter
@Getter
public class ReviewCreateDTO {

    private UUID author;

    private UUID entity;

    private EntityType entityType;

    private int rating;

    private String comment;

}

package org.evently.reviews.repositories;

import org.evently.reviews.models.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewsRepository extends JpaRepository<Review, UUID> {
}

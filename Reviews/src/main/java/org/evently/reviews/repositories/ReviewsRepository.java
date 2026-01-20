package org.evently.reviews.repositories;

import org.evently.reviews.models.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewsRepository extends JpaRepository<Review, UUID> {

    Page<Review> findAllByAuthorId(UUID authorId, PageRequest pageRequest);

    Page<Review> findAllByEntityId(UUID entityId, PageRequest pageRequest);

}

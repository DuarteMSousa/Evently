package org.evently.reviews.repositories;

import org.evently.reviews.models.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewCommentsRepository extends JpaRepository<ReviewComment, UUID> {
}

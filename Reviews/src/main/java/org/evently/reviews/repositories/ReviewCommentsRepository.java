package org.evently.reviews.repositories;

import org.evently.reviews.models.Review;
import org.evently.reviews.models.ReviewComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewCommentsRepository extends JpaRepository<ReviewComment, UUID> {

    Page<ReviewComment> findAllByReview(Review review, PageRequest pageRequest);

}

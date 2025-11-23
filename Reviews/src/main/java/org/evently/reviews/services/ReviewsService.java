package org.evently.reviews.services;

import org.evently.reviews.exceptions.UnexistingReviewException;
import org.evently.reviews.models.Review;
import org.evently.reviews.repositories.ReviewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewsService {

    @Autowired
    private ReviewsRepository reviewsRepository;

    public List<Review> getAll() {
        return reviewsRepository.findAll();
    }

    public Review getReview(UUID id) throws UnexistingReviewException {
        return reviewsRepository
                .findById(id)
                .orElseThrow(() -> new UnexistingReviewException());
    }

    @Transactional
    public Review registerReview(Review review) {
        return reviewsRepository.save(review);
    }

    @Transactional
    public Review updateReview(UUID id, Review review) throws UnexistingReviewException {
        if (!reviewsRepository.existsById(review.getId())) {
            throw new UnexistingReviewException();
        }

        Review existingReview = reviewsRepository.findById(id).orElseThrow(() -> new UnexistingReviewException());

        existingReview.setAuthor(existingReview.getAuthor());
        existingReview.setEntity(existingReview.getEntity());
        existingReview.setEntityType(existingReview.getEntityType());
        existingReview.setRating(review.getRating());
        existingReview.setComment(review.getComment());


        return reviewsRepository.save(existingReview);
    }

    @Transactional
    public void deleteReview(UUID id) throws UnexistingReviewException {
        if (!reviewsRepository.existsById(id)) {
            throw new UnexistingReviewException();
        }

        reviewsRepository.deleteById(id);
    }
}

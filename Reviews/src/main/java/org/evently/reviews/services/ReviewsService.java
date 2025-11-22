package org.evently.reviews.services;

import org.evently.reviews.exceptions.UnexistingReviewException;
import org.evently.reviews.models.Review;
import org.evently.reviews.repositories.ReviewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewsService {

    @Autowired
    private ReviewsRepository reviewsRepository;

    public List<Review> getAll() {
        return reviewsRepository.findAll();
    }

    public Review get(UUID id) throws UnexistingReviewException {
        return reviewsRepository
                .findById(id)
                .orElseThrow(() -> new UnexistingReviewException());
    }

    public Review add(Review review) {
        return reviewsRepository.save(review);
    }

    public Review update(Review review) throws UnexistingReviewException {
        if (!reviewsRepository.existsById(review.getId())) {
            throw new UnexistingReviewException();
        }

        return reviewsRepository.save(review);
    }

    public void delete(UUID id) throws UnexistingReviewException {
        if (!reviewsRepository.existsById(id)) {
            throw new UnexistingReviewException();
        }

        reviewsRepository.deleteById(id);
    }
}

package org.evently.reviews.services;

import org.evently.reviews.exceptions.UnexistingReviewCommentException;
import org.evently.reviews.exceptions.UnexistingReviewException;
import org.evently.reviews.models.ReviewComment;
import org.evently.reviews.repositories.ReviewCommentsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewCommentsService {

    @Autowired
    private ReviewCommentsRepository reviewCommentsRepository;

    @Autowired
    private ReviewsService reviewsService;

    public List<ReviewComment> getAll() {
        return reviewCommentsRepository.findAll();
    }

    public ReviewComment get(UUID id) throws UnexistingReviewCommentException {
        return reviewCommentsRepository
                .findById(id)
                .orElseThrow(() -> new UnexistingReviewCommentException());
    }

    public ReviewComment add(ReviewComment reviewComment) throws UnexistingReviewException {
        if(reviewsService.get(reviewComment.getReview().getId()) == null)
            throw new UnexistingReviewException();

        return reviewCommentsRepository.save(reviewComment);
    }

    public ReviewComment update(ReviewComment reviewComment) throws UnexistingReviewCommentException {
        if (!reviewCommentsRepository.existsById(reviewComment.getId())) {
            throw new UnexistingReviewCommentException();
        }

        return reviewCommentsRepository.save(reviewComment);
    }

    public void delete(UUID id) throws UnexistingReviewCommentException {
        if (!reviewCommentsRepository.existsById(id)) {
            throw new UnexistingReviewCommentException();
        }

        reviewCommentsRepository.deleteById(id);
    }
}

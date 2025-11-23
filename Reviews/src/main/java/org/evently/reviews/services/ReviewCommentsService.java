package org.evently.reviews.services;

import org.evently.reviews.exceptions.UnexistingReviewCommentException;
import org.evently.reviews.exceptions.UnexistingReviewException;
import org.evently.reviews.models.ReviewComment;
import org.evently.reviews.repositories.ReviewCommentsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
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

    public ReviewComment getReviewComment(UUID id) throws UnexistingReviewCommentException {
        return reviewCommentsRepository
                .findById(id)
                .orElseThrow(() -> new UnexistingReviewCommentException());
    }

    @Transactional
    public ReviewComment registerReviewComment(ReviewComment reviewComment) throws UnexistingReviewException {
        if(reviewsService.getReview(reviewComment.getReview().getId()) == null)
            throw new UnexistingReviewException();

        return reviewCommentsRepository.save(reviewComment);
    }

    @Transactional
    public ReviewComment updateReviewComment(ReviewComment reviewComment) throws UnexistingReviewCommentException {
        if (!reviewCommentsRepository.existsById(reviewComment.getId())) {
            throw new UnexistingReviewCommentException();
        }

        return reviewCommentsRepository.save(reviewComment);
    }

    @Transactional
    public void deleteReviewComment(UUID id) throws UnexistingReviewCommentException {
        if (!reviewCommentsRepository.existsById(id)) {
            throw new UnexistingReviewCommentException();
        }

        reviewCommentsRepository.deleteById(id);
    }
}

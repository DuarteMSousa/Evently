package org.evently.reviews.services;

import jakarta.transaction.Transactional;
import org.evently.reviews.exceptions.*;
import org.evently.reviews.models.ReviewComment;
import org.evently.reviews.repositories.ReviewCommentsRepository;
import org.evently.reviews.repositories.ReviewsRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReviewCommentsService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewCommentsService.class);

    private static final Marker COMMENT_CREATE = MarkerFactory.getMarker("COMMENT_CREATE");
    private static final Marker COMMENT_UPDATE = MarkerFactory.getMarker("COMMENT_UPDATE");
    private static final Marker COMMENT_DELETE = MarkerFactory.getMarker("COMMENT_DELETE");
    private static final Marker COMMENT_GET = MarkerFactory.getMarker("COMMENT_GET");
    private static final Marker COMMENT_VALIDATION = MarkerFactory.getMarker("COMMENT_VALIDATION");

    @Autowired
    private ReviewCommentsRepository reviewCommentsRepository;

    @Autowired
    private ReviewsRepository reviewsRepository;

    private final ModelMapper modelMapper = new ModelMapper();

    private void validateComment(ReviewComment comment) {
        logger.debug(COMMENT_VALIDATION, "Validating comment payload");

        if (comment.getAuthorId() == null) {
            logger.warn(COMMENT_VALIDATION, "Missing authorId");
            throw new InvalidReviewCommentUpdateException("Author ID is required");
        }
        if (comment.getComment() == null || comment.getComment().trim().isEmpty()) {
            logger.warn(COMMENT_VALIDATION, "Comment text is empty");
            throw new InvalidReviewCommentUpdateException("Comment text cannot be empty");
        }
        if (comment.getReview() == null || comment.getReview().getId() == null) {
            logger.warn(COMMENT_VALIDATION, "Comment is not linked to a review");
            throw new InvalidReviewCommentUpdateException("A valid Review ID must be associated");
        }
    }

    public ReviewComment getReviewComment(UUID id) {
        logger.debug(COMMENT_GET, "Get comment requested (id={})", id);
        return reviewCommentsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(COMMENT_GET, "Comment not found (id={})", id);
                    return new ReviewCommentNotFoundException("Review comment not found");
                });
    }

    @Transactional
    public ReviewComment registerReviewComment(ReviewComment reviewComment) {
        logger.info(COMMENT_CREATE, "Register comment requested for review (id={})",
                reviewComment.getReview() != null ? reviewComment.getReview().getId() : "NULL");

        validateComment(reviewComment);

        if (!reviewsRepository.existsById(reviewComment.getReview().getId())) {
            logger.warn(COMMENT_CREATE, "Parent review not found (id={})", reviewComment.getReview().getId());
            throw new ReviewNotFoundException("Review not found");
        }

        ReviewComment saved = reviewCommentsRepository.save(reviewComment);
        logger.info(COMMENT_CREATE, "Comment registered successfully (id={})", saved.getId());
        return saved;
    }

    @Transactional
    public ReviewComment updateReviewComment(UUID id, ReviewComment reviewComment) {
        logger.info(COMMENT_UPDATE, "Update comment requested (id={})", id);

        if (reviewComment.getId() != null && !id.equals(reviewComment.getId())) {
            logger.error(COMMENT_UPDATE, "ID mismatch: path={}, body={}", id, reviewComment.getId());
            throw new InvalidReviewCommentUpdateException("Parameter id and body id do not correspond");
        }

        ReviewComment existingComment = reviewCommentsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(COMMENT_UPDATE, "Comment not found for update (id={})", id);
                    return new ReviewCommentNotFoundException("Review Comment not found");
                });

        validateComment(reviewComment);
        modelMapper.map(reviewComment, existingComment);

        ReviewComment updated = reviewCommentsRepository.save(existingComment);
        logger.info(COMMENT_UPDATE, "Comment updated successfully (id={})", updated.getId());
        return updated;
    }

    @Transactional
    public void deleteReviewComment(UUID id) {
        logger.info(COMMENT_DELETE, "Delete comment requested (id={})", id);

        if (!reviewCommentsRepository.existsById(id)) {
            logger.warn(COMMENT_DELETE, "Comment not found for deletion (id={})", id);
            throw new ReviewCommentNotFoundException("Review Comment not found");
        }

        reviewCommentsRepository.deleteById(id);
        logger.info(COMMENT_DELETE, "Comment deleted successfully (id={})", id);
    }

    public Page<ReviewComment> getReviewCommentsByReview(org.evently.reviews.models.Review review, Integer pageNumber, Integer pageSize) {
        if (pageSize > 50 || pageSize < 1) {
            pageSize = 50;
        }

        if (pageNumber < 1) {
            pageNumber = 1;
        }

        logger.debug(COMMENT_GET, "Fetching comments for review (reviewId={}, page={}, size={})",
                review.getId(), pageNumber, pageSize);

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return reviewCommentsRepository.findAllByReview(review, pageable);
    }
}
package org.evently.reviews.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.evently.reviews.clients.UsersClient;
import org.evently.reviews.exceptions.*;
import org.evently.reviews.exceptions.externalServices.UserNotFoundException;
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
    private static final Marker COMMENT_DELETE = MarkerFactory.getMarker("COMMENT_DELETE");
    private static final Marker COMMENT_GET = MarkerFactory.getMarker("COMMENT_GET");
    private static final Marker COMMENT_VALIDATION = MarkerFactory.getMarker("COMMENT_VALIDATION");

    @Autowired
    private ReviewCommentsRepository reviewCommentsRepository;

    @Autowired
    private ReviewsRepository reviewsRepository;

    @Autowired
    private UsersClient usersClient;

    /**
     * Retrieves a review comment by its unique identifier.
     *
     * @param id review comment identifier
     * @return found review comment
     * @throws ReviewCommentNotFoundException if the comment does not exist
     */
    public ReviewComment getReviewComment(UUID id) {
        logger.debug(COMMENT_GET, "Get comment requested (id={})", id);
        return reviewCommentsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(COMMENT_GET, "Comment not found (id={})", id);
                    return new ReviewCommentNotFoundException("Review comment not found");
                });
    }

    /**
     * Registers a new review comment after validating its data and related entities.
     *
     * @param reviewComment review comment to be registered
     * @return persisted review comment
     * @throws InvalidReviewCommentException if the comment data is invalid
     * @throws UserNotFoundException if the author user does not exist
     * @throws ReviewNotFoundException if the associated review does not exist
     * @throws ExternalServiceException if the Users service is unavailable or returns an error
     */
    @Transactional
    public ReviewComment registerReviewComment(ReviewComment reviewComment) {
        logger.info(COMMENT_CREATE, "Register comment requested for review (id={})",
                reviewComment.getReview() != null ? reviewComment.getReview().getId() : "NULL");

        validateComment(reviewComment);

        try {
            usersClient.getUser(reviewComment.getAuthorId());
        } catch (FeignException.NotFound e) {
            logger.warn(COMMENT_CREATE, "(ReviewCommentsService): User not found in Users service");
            throw new UserNotFoundException(
                    "(ReviewCommentsService): User not found in Users service");
        } catch (FeignException e) {
            logger.error(COMMENT_CREATE, "(ReviewCommentsService): Users service error", e);
            throw new ExternalServiceException(
                    "(ReviewCommentsService): Users service error");
        }

        if (!reviewsRepository.existsById(reviewComment.getReview().getId())) {
            logger.warn(COMMENT_CREATE, "Parent review not found (id={})", reviewComment.getReview().getId());
            throw new ReviewNotFoundException("Review not found");
        }

        ReviewComment saved = reviewCommentsRepository.save(reviewComment);
        logger.info(COMMENT_CREATE, "Comment registered successfully (id={})", saved.getId());
        return saved;
    }

    /**
     * Deletes a review comment by its unique identifier.
     *
     * @param id review comment identifier
     * @throws ReviewCommentNotFoundException if the comment does not exist
     */
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

    /**
     * Retrieves a paginated list of review comments associated with a review.
     *
     * @param review review entity
     * @param pageNumber page number (1-based)
     * @param pageSize page size
     * @return page of review comments for the given review
     */
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

    /**
     * Validates all required fields of a review comment before registration or update.
     *
     * @param comment review comment to validate
     * @throws InvalidReviewCommentException if any required field is missing or invalid
     */
    private void validateComment(ReviewComment comment) {
        logger.debug(COMMENT_VALIDATION, "Validating comment payload");

        if (comment.getAuthorId() == null) {
            logger.warn(COMMENT_VALIDATION, "Missing authorId");
            throw new InvalidReviewCommentException("Author ID is required");
        }
        if (comment.getComment() == null || comment.getComment().trim().isEmpty()) {
            logger.warn(COMMENT_VALIDATION, "Comment text is empty");
            throw new InvalidReviewCommentException("Comment text cannot be empty");
        }
        if (comment.getReview() == null || comment.getReview().getId() == null) {
            logger.warn(COMMENT_VALIDATION, "Comment is not linked to a review");
            throw new InvalidReviewCommentException("A valid Review ID must be associated");
        }
    }

}
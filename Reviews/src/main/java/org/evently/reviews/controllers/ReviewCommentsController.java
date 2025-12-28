package org.evently.reviews.controllers;

import org.evently.reviews.dtos.reviewComments.ReviewCommentCreateDTO;
import org.evently.reviews.dtos.reviewComments.ReviewCommentDTO;
import org.evently.reviews.exceptions.ReviewCommentNotFoundException;
import org.evently.reviews.models.Review;
import org.evently.reviews.models.ReviewComment;
import org.evently.reviews.services.ReviewCommentsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/reviews/comments")
public class ReviewCommentsController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewCommentsController.class);

    private static final Marker COMMENT_GET = MarkerFactory.getMarker("COMMENT_GET");
    private static final Marker COMMENT_CREATE = MarkerFactory.getMarker("COMMENT_CREATE");
    private static final Marker COMMENT_DELETE = MarkerFactory.getMarker("COMMENT_DELETE");

    @Autowired
    private ReviewCommentsService reviewCommentsService;

    @GetMapping("/get-comment/{id}")
    public ResponseEntity<?> getReviewComment(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Comment found.
         * 404 NOT_FOUND - Comment does not exist.
         * 400 BAD_REQUEST - Generic error.
         */

        logger.info(COMMENT_GET, "Method getReviewComment entered for ID: {}", id);
        try {
            ReviewComment comment = reviewCommentsService.getReviewComment(id);
            logger.info(COMMENT_GET, "200 OK returned, comment found");
            return ResponseEntity.ok(convertToDTO(comment));
        } catch (ReviewCommentNotFoundException e) {
            logger.warn(COMMENT_GET, "404 NOT_FOUND: Comment {} not found", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(COMMENT_GET, "400 BAD_REQUEST: Exception caught while getting comment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/review/{reviewId}")
    public ResponseEntity<Page<ReviewCommentDTO>> getCommentsByReview(
            @PathVariable("reviewId") UUID reviewId,
            @RequestParam(value="page", defaultValue = "1") Integer page,
            @RequestParam(value="size", defaultValue = "50") Integer size) {
        /* HttpStatus(produces)
         * 200 OK - Paginated list of comments for the specified review retrieved successfully.
         */

        logger.info(COMMENT_GET, "Method getCommentsByReview entered for Review ID: {}", reviewId);

        Review review = new Review();
        review.setId(reviewId);

        Page<ReviewComment> commentPage = reviewCommentsService.getReviewCommentsByReview(review, page, size);
        Page<ReviewCommentDTO> dtoPage = commentPage.map(this::convertToDTO);

        logger.info(COMMENT_GET, "200 OK returned, paginated comments retrieved");
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/register-comment")
    public ResponseEntity<?> registerReviewComment(@RequestBody ReviewCommentCreateDTO commentDTO) {
        /* HttpStatus(produces)
         * 201 CREATED - Comment created successfully.
         * 404 NOT_FOUND - The Review to which the comment belongs does not exist.
         * 400 BAD_REQUEST - Invalid data provided.
         */

        logger.info(COMMENT_CREATE, "Method registerReviewComment entered");
        try {
            ReviewComment commentRequest = new ReviewComment();
            commentRequest.setAuthorId(commentDTO.getAuthor());
            commentRequest.setComment(commentDTO.getComment());
            commentRequest.setCreatedAt(new Date());

            Review review = new Review();
            review.setId(commentDTO.getReviewId());
            commentRequest.setReview(review);

            ReviewComment saved = reviewCommentsService.registerReviewComment(commentRequest);
            logger.info(COMMENT_CREATE, "201 CREATED returned, comment registered");
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
        } catch (Exception e) {
            logger.error(COMMENT_CREATE, "400 BAD_REQUEST: Exception caught while registering comment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete-comment/{id}")
    public ResponseEntity<?> deleteReviewComment(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 204 NO_CONTENT - Comment removed successfully.
         * 404 NOT_FOUND - Comment not found.
         */

        logger.info(COMMENT_DELETE, "Method deleteReviewComment entered for ID: {}", id);
        try {
            reviewCommentsService.deleteReviewComment(id);
            logger.info(COMMENT_DELETE, "204 NO_CONTENT returned, comment deleted");
            return ResponseEntity.noContent().build();
        } catch (ReviewCommentNotFoundException e) {
            logger.warn(COMMENT_DELETE, "404 NOT_FOUND: Comment {} not found for deletion", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(COMMENT_DELETE, "400 BAD_REQUEST: Exception caught while deleting comment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    private ReviewCommentDTO convertToDTO(ReviewComment comment) {
        ReviewCommentDTO dto = new ReviewCommentDTO();
        dto.setId(comment.getId());
        dto.setAuthor(comment.getAuthorId());
        dto.setComment(comment.getComment());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        if (comment.getReview() != null) {
            dto.setReviewId(comment.getReview().getId());
        }
        return dto;
    }
}
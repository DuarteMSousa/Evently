package org.evently.reviews.controllers;

import org.evently.reviews.dtos.reviewComments.ReviewCommentCreateDTO;
import org.evently.reviews.dtos.reviewComments.ReviewCommentDTO;
import org.evently.reviews.dtos.reviewComments.ReviewCommentUpdateDTO;
import org.evently.reviews.exceptions.ReviewCommentNotFoundException;
import org.evently.reviews.models.Review;
import org.evently.reviews.models.ReviewComment;
import org.evently.reviews.services.ReviewCommentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reviews/comments")
public class ReviewCommentsController {

    @Autowired
    private ReviewCommentsService reviewCommentsService;

    @GetMapping("/get-comment/{id}")
    public ResponseEntity<?> getReviewComment(@PathVariable("id") UUID id) {
        /*
         * 200 OK - Comment found.
         * 404 NOT_FOUND - Comment does not exist.
         * 400 BAD_REQUEST - Generic error.
         */
        try {
            ReviewComment comment = reviewCommentsService.getReviewComment(id);
            return ResponseEntity.ok(convertToDTO(comment));
        } catch (ReviewCommentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/review/{reviewId}")
    public ResponseEntity<Page<ReviewCommentDTO>> getCommentsByReview(
            @PathVariable("reviewId") UUID reviewId,
            @RequestParam(value="page", defaultValue = "0") Integer page,
            @RequestParam(value="size", defaultValue = "10") Integer size) {
        /*
         * 200 OK - Paginated list of comments for the specified review retrieved successfully.
         */
        Review review = new Review();
        review.setId(reviewId);

        Page<ReviewComment> commentPage = reviewCommentsService.getReviewCommentsByReview(review, page, size);
        Page<ReviewCommentDTO> dtoPage = commentPage.map(this::convertToDTO);

        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/register-comment")
    public ResponseEntity<?> registerReviewComment(@RequestBody ReviewCommentCreateDTO commentDTO) {
        /*
         * 201 CREATED - Comment created successfully.
         * 404 NOT_FOUND - The Review to which the comment belongs does not exist.
         * 400 BAD_REQUEST - Invalid data provided.
         */
        try {
            ReviewComment commentRequest = new ReviewComment();
            commentRequest.setAuthorId(commentDTO.getAuthor());
            commentRequest.setComment(commentDTO.getComment());

            Review review = new Review();
            review.setId(commentDTO.getReviewId());
            commentRequest.setReview(review);

            ReviewComment saved = reviewCommentsService.registerReviewComment(commentRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/update-comment/{id}")
    public ResponseEntity<?> updateReviewComment(@PathVariable("id") UUID id, @RequestBody ReviewCommentUpdateDTO dto) {
        /*
         * 200 OK - Comment updated.
         * 404 NOT_FOUND - Comment not found.
         * 400 BAD_REQUEST - Invalid data.
         */
        try {
            ReviewComment updateData = new ReviewComment();
            updateData.setComment(dto.getComment());

            ReviewComment updated = reviewCommentsService.updateReviewComment(id, updateData);
            return ResponseEntity.ok(convertToDTO(updated));
        } catch (ReviewCommentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete-comment/{id}")
    public ResponseEntity<?> deleteReviewComment(@PathVariable("id") UUID id) {
        /*
         * 204 NO_CONTENT - Comment removed successfully.
         * 404 NOT_FOUND - Comment not found.
         */
        try {
            reviewCommentsService.deleteReviewComment(id);
            return ResponseEntity.noContent().build();
        } catch (ReviewCommentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    private ReviewCommentDTO convertToDTO(ReviewComment comment) {
        ReviewCommentDTO dto = new ReviewCommentDTO();
        dto.setId(comment.getId());
        dto.setAuthor(comment.getAuthorId());
        dto.setComment(comment.getComment());
        if (comment.getReview() != null) {
            dto.setReviewId(comment.getReview().getId());
        }
        return dto;
    }
}
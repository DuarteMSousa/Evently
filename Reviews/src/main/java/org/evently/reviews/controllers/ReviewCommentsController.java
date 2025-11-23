package org.evently.reviews.controllers;

import org.evently.reviews.dtos.reviewComments.ReviewCommentCreateDTO;
import org.evently.reviews.dtos.reviewComments.ReviewCommentDTO;
import org.evently.reviews.dtos.reviewComments.ReviewCommentUpdateDTO;
import org.evently.reviews.exceptions.UnexistingReviewCommentException;
import org.evently.reviews.exceptions.UnexistingReviewException;
import org.evently.reviews.models.Review;
import org.evently.reviews.models.ReviewComment;
import org.evently.reviews.services.ReviewCommentsService;
import org.evently.reviews.services.ReviewsService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/review-comments")
public class ReviewCommentsController {

    @Autowired
    private ReviewCommentsService reviewCommentsService;

    @Autowired
    private ReviewsService reviewsService;

    private final ModelMapper modelMapper;

    public ReviewCommentsController() {
        this.modelMapper = new ModelMapper();
    }

    @GetMapping("/get-comment/{id}")
    public ResponseEntity<?> getReviewComment(@PathVariable("id") UUID id) {
        ReviewComment comment;

        try {
            comment = reviewCommentsService.getReviewComment(id);
        } catch (UnexistingReviewCommentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(modelMapper.map(comment, ReviewCommentDTO.class));
    }

    @PostMapping("/register-comment/{reviewId}")
    public ResponseEntity<?> registerReviewComment(@PathVariable("reviewId") UUID reviewId ,@RequestBody ReviewCommentCreateDTO commentDTO) throws UnexistingReviewException {
        ReviewComment newComment;

        Review review = reviewsService.getReview(reviewId);

        newComment = new ReviewComment();
        newComment.setAuthor(commentDTO.getAuthor());
        newComment.setReview(review);
        newComment.setComment(commentDTO.getComment());

        try {
            newComment = reviewCommentsService.registerReviewComment(newComment);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }

        return new ResponseEntity<>(
                modelMapper.map(newComment, ReviewCommentDTO.class),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/update-comment")
    public ResponseEntity<?> updateReviewComment(@RequestBody ReviewCommentUpdateDTO commentDTO) {
        ReviewComment updatedComment;

        try {
            updatedComment = reviewCommentsService.updateReviewComment(modelMapper.map(commentDTO, ReviewComment.class));
        } catch (UnexistingReviewCommentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }

        return new ResponseEntity<>(
                modelMapper.map(updatedComment, ReviewCommentDTO.class),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/delete-comment/{id}")
    public ResponseEntity<?> deleteReviewComment(@PathVariable("id") UUID id) {
        try {
            reviewCommentsService.deleteReviewComment(id);
        } catch (UnexistingReviewCommentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

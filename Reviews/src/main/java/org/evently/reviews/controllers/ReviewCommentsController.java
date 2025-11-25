package org.evently.reviews.controllers;

import org.evently.reviews.dtos.reviewComments.ReviewCommentCreateDTO;
import org.evently.reviews.dtos.reviewComments.ReviewCommentDTO;
import org.evently.reviews.dtos.reviewComments.ReviewCommentUpdateDTO;

import org.evently.reviews.exceptions.ReviewCommentNotFoundException;
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
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - undefined error
         * 404 NOT_FOUND - comment not found
         */

        ReviewComment comment;

        try {
            comment = reviewCommentsService.getReviewComment(id);
        } catch (ReviewCommentNotFoundException e) {
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

    @PostMapping("/register-comment")
    public ResponseEntity<?> registerReviewComment(@RequestBody ReviewCommentCreateDTO commentDTO)
            throws ReviewCommentNotFoundException {
        /* HttpStatus(produces)
         * 201 CREATED - Request processed as expected.
         * 400 BAD_REQUEST - undefined error
         * 404 NOT_FOUND - review not found
         */

        ReviewComment newComment;

        try {
            newComment = reviewCommentsService.registerReviewComment(modelMapper.map(commentDTO, ReviewComment.class));
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

    @PutMapping("/update-comment/{id}")
    public ResponseEntity<?> updateReviewComment(@PathVariable("id") UUID id, @RequestBody ReviewCommentUpdateDTO reviewCommentUpdateDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - undefined error
         * 404 NOT_FOUND - comment not found
         */

        ReviewComment updatedReviewComment;

        try {
            updatedReviewComment = reviewCommentsService.updateReviewComment(id ,modelMapper.map(reviewCommentUpdateDTO, ReviewComment.class));
        } catch (ReviewCommentNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }

        return new ResponseEntity<>(modelMapper.map(updatedReviewComment, ReviewCommentDTO.class), HttpStatus.OK);
    }

    @DeleteMapping("/delete-comment/{id}")
    public ResponseEntity<?> deleteReviewComment(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 204 NO_CONTENT - Request processed as expected.
         * 400 BAD_REQUEST - undefined error
         * 404 NOT_FOUND - comment not found
         */

        try {
            reviewCommentsService.deleteReviewComment(id);
        } catch (ReviewCommentNotFoundException e) {
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

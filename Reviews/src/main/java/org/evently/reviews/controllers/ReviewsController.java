package org.evently.reviews.controllers;

import org.evently.reviews.dtos.reviews.ReviewCreateDTO;
import org.evently.reviews.dtos.reviews.ReviewDTO;
import org.evently.reviews.dtos.reviews.ReviewUpdateDTO;
import org.evently.reviews.exceptions.ReviewNotFoundException;
import org.evently.reviews.models.Review;
import org.evently.reviews.services.ReviewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reviews")
public class ReviewsController {

    @Autowired
    private ReviewsService reviewService;

    @GetMapping("/get-review/{id}")
    public ResponseEntity<?> getReview(@PathVariable("id") UUID id) {
        /*
         * 200 OK - Review found.
         * 404 NOT_FOUND - No review exists with the provided ID.
         * 400 BAD_REQUEST - Unexpected error during processing.
         */
        try {
            Review review = reviewService.getReview(id);
            return ResponseEntity.ok(convertToDTO(review));
        } catch (ReviewNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<Page<ReviewDTO>> getReviewsByAuthor(
            @PathVariable("authorId") UUID authorId,
            @RequestParam(value= "page", defaultValue = "0") Integer page,
            @RequestParam(value= "size", defaultValue = "10") Integer size) {
        /*
         * 200 OK - Paginated list of reviews by author retrieved successfully.
         */
        Page<Review> reviewPage = reviewService.getReviewsByAuthor(authorId, page, size);
        Page<ReviewDTO> dtoPage = reviewPage.map(this::convertToDTO);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/entity/{entityId}")
    public ResponseEntity<Page<ReviewDTO>> getReviewsByEntity(
            @PathVariable("entityId") UUID entityId,
            @RequestParam(value="page", defaultValue = "0") Integer page,
            @RequestParam(value="size", defaultValue = "10") Integer size) {
        /*
         * 200 OK - Paginated list of reviews by entity retrieved successfully.
         */
        Page<Review> reviewPage = reviewService.getReviewsByEntity(entityId, page, size);
        Page<ReviewDTO> dtoPage = reviewPage.map(this::convertToDTO);
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/register-review")
    public ResponseEntity<?> registerReview(@RequestBody ReviewCreateDTO reviewDTO) {
        /*
         * 201 CREATED - Review registered successfully.
         * 400 BAD_REQUEST - Invalid data (e.g., rating out of bounds) or system error.
         */
        try {
            Review reviewRequest = new Review();
            reviewRequest.setAuthorId(reviewDTO.getAuthorId());
            reviewRequest.setEntityId(reviewDTO.getEntityId());
            reviewRequest.setEntityType(reviewDTO.getEntityType());
            reviewRequest.setRating(reviewDTO.getRating());
            reviewRequest.setComment(reviewDTO.getComment());

            Review savedReview = reviewService.registerReview(reviewRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedReview));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/update-review/{id}")
    public ResponseEntity<?> updateReview(@PathVariable("id") UUID id, @RequestBody ReviewUpdateDTO reviewDTO) {
        /*
         * 200 OK - Review updated successfully.
         * 404 NOT_FOUND - Review not found for the provided ID.
         * 400 BAD_REQUEST - Validation error or ID mismatch.
         */
        try {
            Review updateData = new Review();
            updateData.setRating(reviewDTO.getRating());
            updateData.setComment(reviewDTO.getComment());

            Review updated = reviewService.updateReview(id, updateData);
            return ResponseEntity.ok(convertToDTO(updated));
        } catch (ReviewNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete-review/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable("id") UUID id) {
        /*
         * 204 NO_CONTENT - Review deleted successfully.
         * 404 NOT_FOUND - Review does not exist.
         * 400 BAD_REQUEST - Error processing the request.
         */
        try {
            reviewService.deleteReview(id);
            return ResponseEntity.noContent().build();
        } catch (ReviewNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    private ReviewDTO convertToDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setAuthorId(review.getAuthorId());
        dto.setEntityId(review.getEntityId());
        dto.setEntityType(review.getEntityType());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setReviewComments(review.getComments());
        return dto;
    }
}
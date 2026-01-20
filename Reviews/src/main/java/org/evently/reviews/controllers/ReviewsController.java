package org.evently.reviews.controllers;

import org.evently.reviews.dtos.reviews.ReviewCreateDTO;
import org.evently.reviews.dtos.reviews.ReviewDTO;
import org.evently.reviews.exceptions.ReviewNotFoundException;
import org.evently.reviews.models.Review;
import org.evently.reviews.services.ReviewsService;
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
@RequestMapping("/reviews")
public class ReviewsController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewsController.class);

    private static final Marker REVIEW_GET = MarkerFactory.getMarker("REVIEW_GET");
    private static final Marker REVIEW_CREATE = MarkerFactory.getMarker("REVIEW_CREATE");
    private static final Marker REVIEW_DELETE = MarkerFactory.getMarker("REVIEW_DELETE");

    @Autowired
    private ReviewsService reviewService;

    @GetMapping("/get-review/{id}")
    public ResponseEntity<?> getReview(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Review found.
         * 404 NOT_FOUND - No review exists with the provided ID.
         * 400 BAD_REQUEST - Unexpected error during processing.
         */

        logger.info(REVIEW_GET, "Method getReview entered for ID: {}", id);
        try {
            Review review = reviewService.getReview(id);
            logger.info(REVIEW_GET, "200 OK returned, review found");
            return ResponseEntity.ok(convertToDTO(review));
        } catch (ReviewNotFoundException e) {
            logger.warn(REVIEW_GET, "404 NOT_FOUND: Review {} not found", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(REVIEW_GET, "400 BAD_REQUEST: Exception caught while getting review: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/author/{authorId}/{pageNumber}/{pageSize}")
    public ResponseEntity<Page<ReviewDTO>> getReviewsByAuthor(
            @PathVariable("authorId") UUID authorId,
            @PathVariable("pageNumber") Integer pageNumber, @PathVariable("pageSize") Integer pageSize) {
        /* HttpStatus(produces)
         * 200 OK - Paginated list of reviews by author retrieved successfully.
         */

        logger.info(REVIEW_GET, "Method getReviewsByAuthor entered for Author: {}", authorId);
        Page<Review> reviewPage = reviewService.getReviewsByAuthor(authorId, pageNumber, pageSize);
        Page<ReviewDTO> dtoPage = reviewPage.map(this::convertToDTO);

        logger.info(REVIEW_GET, "200 OK returned, author reviews retrieved");
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/entity/{entityId}/{pageNumber}/{pageSize}")
    public ResponseEntity<Page<ReviewDTO>> getReviewsByEntity(
            @PathVariable("entityId") UUID entityId,
            @PathVariable("pageNumber") Integer pageNumber, @PathVariable("pageSize") Integer pageSize) {
        /* HttpStatus(produces)
         * 200 OK - Paginated list of reviews by entity retrieved successfully.
         */

        logger.info(REVIEW_GET, "Method getReviewsByEntity entered for Entity: {}", entityId);
        Page<Review> reviewPage = reviewService.getReviewsByEntity(entityId, pageNumber, pageSize);
        Page<ReviewDTO> dtoPage = reviewPage.map(this::convertToDTO);

        logger.info(REVIEW_GET, "200 OK returned, entity reviews retrieved");
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/register-review")
    public ResponseEntity<?> registerReview(@RequestBody ReviewCreateDTO reviewDTO) {
        /* HttpStatus(produces)
         * 201 CREATED - Review registered successfully.
         * 400 BAD_REQUEST - Invalid data or system error.
         */

        logger.info(REVIEW_CREATE, "Method registerReview entered");
        try {
            Review reviewRequest = new Review();
            reviewRequest.setAuthorId(reviewDTO.getAuthorId());
            reviewRequest.setEntityId(reviewDTO.getEntityId());
            reviewRequest.setEntityType(reviewDTO.getEntityType());
            reviewRequest.setRating(reviewDTO.getRating());
            reviewRequest.setComment(reviewDTO.getComment());
            reviewRequest.setCreatedAt(new Date());

            Review savedReview = reviewService.registerReview(reviewRequest);
            logger.info(REVIEW_CREATE, "201 CREATED returned, review registered");
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedReview));
        } catch (Exception e) {
            logger.error(REVIEW_CREATE, "400 BAD_REQUEST: Exception caught while registering review: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete-review/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 204 NO_CONTENT - Review deleted successfully.
         * 404 NOT_FOUND - Review does not exist.
         * 400 BAD_REQUEST - Error processing the request.
         */

        logger.info(REVIEW_DELETE, "Method deleteReview entered for ID: {}", id);
        try {
            reviewService.deleteReview(id);
            logger.info(REVIEW_DELETE, "204 NO_CONTENT returned, review deleted");
            return ResponseEntity.noContent().build();
        } catch (ReviewNotFoundException e) {
            logger.warn(REVIEW_DELETE, "404 NOT_FOUND: Review {} not found", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(REVIEW_DELETE, "400 BAD_REQUEST: Exception caught while deleting review: {}", e.getMessage());
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
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        return dto;
    }

}
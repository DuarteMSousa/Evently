package org.evently.reviews.controllers;

import org.evently.reviews.dtos.reviews.ReviewCreateDTO;
import org.evently.reviews.dtos.reviews.ReviewDTO;
import org.evently.reviews.dtos.reviews.ReviewUpdateDTO;
import org.evently.reviews.exceptions.UnexistingReviewException;
import org.evently.reviews.models.Review;
import org.evently.reviews.services.ReviewsService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reviews")
public class ReviewsController {

    @Autowired
    private ReviewsService reviewService;

    private ModelMapper modelMapper;

    public ReviewsController() {
        modelMapper = new ModelMapper();
    }

    @GetMapping("/get-review/{id}")
    public ResponseEntity<?> getReview(@PathVariable("id") UUID id) {
        Review review;

        try {
            review = reviewService.getReview(id);
        } catch (UnexistingReviewException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(modelMapper.map(review, ReviewDTO.class));
    }

    @PostMapping("/register-review")
    public ResponseEntity<?> registerReview(@RequestBody ReviewCreateDTO reviewDTO) {
        Review newReview;

        try {
            newReview = reviewService.registerReview(modelMapper.map(reviewDTO, Review.class));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }

        return new ResponseEntity<>(modelMapper.map(newReview, ReviewDTO.class), HttpStatus.CREATED);
    }

    @PutMapping("/update-review/{id}")
    public ResponseEntity<?> updateReview(@PathVariable("id") UUID id, @RequestBody ReviewUpdateDTO reviewDTO) {
        Review updatedReview;

        try {
            updatedReview = reviewService.updateReview(id ,modelMapper.map(reviewDTO, Review.class));
        } catch (UnexistingReviewException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }

        return new ResponseEntity<>(modelMapper.map(updatedReview, ReviewDTO.class), HttpStatus.OK);
    }

    @DeleteMapping("/delete-review/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable("id") UUID id) {
        try {
            reviewService.deleteReview(id);
        } catch (UnexistingReviewException e) {
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
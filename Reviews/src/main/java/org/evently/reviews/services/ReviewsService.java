package org.evently.reviews.services;

import org.evently.reviews.exceptions.InvalidReviewUpdateException;
import org.evently.reviews.exceptions.ReviewAlreadyExistsException;
import org.evently.reviews.exceptions.ReviewNotFoundException;
import org.evently.reviews.models.Review;
import org.evently.reviews.repositories.ReviewsRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
public class ReviewsService {

    @Autowired
    private ReviewsRepository reviewsRepository;

    private ModelMapper modelMapper = new ModelMapper();

    public Review getReview(UUID id) {
        return reviewsRepository
                .findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));
    }

    @Transactional
    public Review registerReview(Review review) {
        return reviewsRepository.save(review);
    }

    @Transactional
    public Review updateReview(UUID id, Review review) {
        if (!id.equals(review.getId())) {
            throw new InvalidReviewUpdateException("Parameter id and body id do not correspond");
        }

        Review existingEvent = reviewsRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));

        //VERIFICAR SE ALTERA CORRETAMENTE
        modelMapper.map(review, existingEvent);

        return reviewsRepository.save(existingEvent);
    }

    @Transactional
    public void deleteReview(UUID id) {
        if (!reviewsRepository.existsById(id)) {
            throw new ReviewNotFoundException("Review not found");
        }

        reviewsRepository.deleteById(id);
    }

    public Page<Review> getReviewsByAuthor(UUID authorId,Integer pageNumber, Integer pageSize) {
        if(pageSize>50){
            pageSize = 50;
        }
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return reviewsRepository.findAllByAuthorId(authorId,pageable);
    }

    public Page<Review> getReviewsByEntity(UUID entityId,Integer pageNumber, Integer pageSize) {
        if(pageSize>50){
            pageSize = 50;
        }
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return reviewsRepository.findAllByAuthorId(entityId,pageable);
    }
}

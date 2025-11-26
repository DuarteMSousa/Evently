package org.evently.reviews.services;

import jakarta.transaction.Transactional;
import org.evently.reviews.exceptions.InvalidReviewCommentUpdateException;
import org.evently.reviews.exceptions.ReviewCommentNotFoundException;
import org.evently.reviews.exceptions.ReviewNotFoundException;
import org.evently.reviews.models.Review;
import org.evently.reviews.models.ReviewComment;
import org.evently.reviews.repositories.ReviewCommentsRepository;
import org.evently.reviews.repositories.ReviewsRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReviewCommentsService {

    @Autowired
    private ReviewCommentsRepository reviewCommentsRepository;

    @Autowired
    private ReviewsRepository reviewsRepository;

    private ModelMapper modelMapper = new ModelMapper();

    public ReviewComment getReviewComment(UUID id) {
        return reviewCommentsRepository
                .findById(id)
                .orElseThrow(() -> new ReviewCommentNotFoundException("Review comment not found"));
    }

    @Transactional
    public ReviewComment registerReviewComment(ReviewComment reviewComment) {
        if(!reviewsRepository.getReviewById(reviewComment.getReview().getId()))
            throw new ReviewNotFoundException("Review not found");

        return reviewCommentsRepository.save(reviewComment);
    }

    @Transactional
    public ReviewComment updateReviewComment(UUID id, ReviewComment reviewComment) {
        if (!id.equals(reviewComment.getId())) {
            throw new InvalidReviewCommentUpdateException("Parameter id and body id do not correspond");
        }

        ReviewComment existingReviewComment = reviewCommentsRepository.findById(id)
                .orElseThrow(() -> new ReviewCommentNotFoundException("Review Comment not found"));

        //VERIFICAR SE ALTERA CORRETAMENTE
        modelMapper.map(reviewComment, existingReviewComment);


        return reviewCommentsRepository.save(existingReviewComment);
    }

    @Transactional
    public void deleteReviewComment(UUID id) {
        if (!reviewCommentsRepository.existsById(id)) {
            throw new ReviewNotFoundException("Review Comment not found");
        }

        reviewCommentsRepository.deleteById(id);
    }

    public Page<ReviewComment> getReviewCommentsByReview(Review review, Integer pageNumber, Integer pageSize) {
        if(pageSize>50){
            pageSize = 50;
        }
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return reviewCommentsRepository.findAllByReview(review,pageable);
    }
}

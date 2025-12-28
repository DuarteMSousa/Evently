package org.evently.reviews.services;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.evently.reviews.clients.EventsClient;
import org.evently.reviews.clients.OrganizationsClient;
import org.evently.reviews.clients.UsersClient;
import org.evently.reviews.clients.VenuesClient;
import org.evently.reviews.exceptions.ExternalServiceException;
import org.evently.reviews.exceptions.InvalidReviewUpdateException;
import org.evently.reviews.exceptions.ReviewNotFoundException;
import org.evently.reviews.exceptions.externalServices.UserNotFoundException;
import org.evently.reviews.models.Review;
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
public class ReviewsService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewsService.class);

    private static final Marker REVIEW_CREATE = MarkerFactory.getMarker("REVIEW_CREATE");
    private static final Marker REVIEW_UPDATE = MarkerFactory.getMarker("REVIEW_UPDATE");
    private static final Marker REVIEW_DELETE = MarkerFactory.getMarker("REVIEW_DELETE");
    private static final Marker REVIEW_GET = MarkerFactory.getMarker("REVIEW_GET");
    private static final Marker REVIEW_VALIDATION = MarkerFactory.getMarker("REVIEW_VALIDATION");

    @Autowired
    private ReviewsRepository reviewsRepository;

    @Autowired
    private EventsClient eventsClient;

    @Autowired
    private UsersClient usersClient;

    @Autowired
    private VenuesClient venuesClient;

    @Autowired
    private OrganizationsClient organizationsClient;

    private final ModelMapper modelMapper = new ModelMapper();

    public Review getReview(UUID id) {
        logger.debug(REVIEW_GET, "Get review requested (id={})", id);
        return reviewsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(REVIEW_GET, "Review not found (id={})", id);
                    return new ReviewNotFoundException("Review not found");
                });
    }

    @Transactional
    public Review registerReview(Review review) {
        logger.info(REVIEW_CREATE, "Register review requested (authorId={}, entityId={})",
                review.getAuthorId(), review.getEntityId());

        validateReview(review);

        switch (review.getEntityType()) {
            case EVENT:
                try {
                    eventsClient.searchEvents(review.getEntityId());
                } catch (FeignException.NotFound e) {
                    logger.warn(REVIEW_CREATE, "(ReviewsService): Event not found in Events service");
                    throw new InvalidReviewUpdateException(
                            "(ReviewsService): Event not found in Events service");
                } catch (FeignException e) {
                    logger.error(REVIEW_CREATE, "(ReviewsService): Events service error", e);
                    throw new ExternalServiceException(
                            "(ReviewsService): Events service error");
                }
                break;
            case VENUE:
                try {
                    venuesClient.getVenue(review.getEntityId());
                } catch (FeignException.NotFound e) {
                    logger.warn(REVIEW_CREATE, "(ReviewsService): Venue not found in Venues service");
                    throw new InvalidReviewUpdateException(
                            "(ReviewsService): Venue not found in Venues service");
                } catch (FeignException e) {
                    logger.error(REVIEW_CREATE, "(ReviewsService): Venues service error", e);
                    throw new ExternalServiceException(
                            "(ReviewsService): Venues service error");
                }
                break;
            case ORGANIZATION:
                try {
                    organizationsClient.getOrganization(review.getEntityId());
                } catch (FeignException.NotFound e) {
                    logger.warn(REVIEW_CREATE, "(ReviewsService): Organization not found in Organizations service");
                    throw new InvalidReviewUpdateException(
                            "(ReviewsService): Organization not found in Organizations service");
                } catch (FeignException e) {
                    logger.error(REVIEW_CREATE, "(ReviewsService): Organizations service error", e);
                    throw new ExternalServiceException(
                            "(ReviewsService): Organizations service error");
                }
                break;
            default:
                throw new InvalidReviewUpdateException("Unknown entity type: " + review.getEntityType());
        }

        try {
            usersClient.getUser(review.getAuthorId());
        } catch (FeignException.NotFound e) {
            logger.warn(REVIEW_CREATE, "(ReviewsService): User not found in Users service");
            throw new UserNotFoundException(
                    "(ReviewsService): User not found in Users service");
        } catch (FeignException e) {
            logger.error(REVIEW_CREATE, "(ReviewsService): Users service error", e);
            throw new ExternalServiceException(
                    "(ReviewsService): Users service error");
        }

        Review saved = reviewsRepository.save(review);
        logger.info(REVIEW_CREATE, "Review registered successfully (id={})", saved.getId());
        return saved;
    }

    @Transactional
    public Review updateReview(UUID id, Review review) {
        logger.info(REVIEW_UPDATE, "Update review requested (id={})", id);

        if (review.getId() != null && !id.equals(review.getId())) {
            logger.error(REVIEW_UPDATE, "ID mismatch: path={}, body={}", id, review.getId());
            throw new InvalidReviewUpdateException("Parameter id and body id do not correspond");
        }

        Review existingReview = reviewsRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn(REVIEW_UPDATE, "Review not found for update (id={})", id);
                    return new ReviewNotFoundException("Review not found");
                });

        validateReview(review);
        modelMapper.map(review, existingReview);

        Review updated = reviewsRepository.save(existingReview);
        logger.info(REVIEW_UPDATE, "Review updated successfully (id={})", updated.getId());
        return updated;
    }

    @Transactional
    public void deleteReview(UUID id) {
        logger.info(REVIEW_DELETE, "Delete review requested (id={})", id);

        if (!reviewsRepository.existsById(id)) {
            logger.warn(REVIEW_DELETE, "Review not found for deletion (id={})", id);
            throw new ReviewNotFoundException("Review not found");
        }

        reviewsRepository.deleteById(id);
        logger.info(REVIEW_DELETE, "Review deleted successfully (id={})", id);
    }

    public Page<Review> getReviewsByAuthor(UUID authorId, Integer pageNumber, Integer pageSize) {
        if (pageSize > 50 || pageSize < 1) {
            pageSize = 50;
        }

        if (pageNumber < 1) {
            pageNumber = 1;
        }

        logger.debug(REVIEW_GET, "Fetching reviews by author (authorId={}, page={}, size={})", authorId, pageNumber, pageSize);

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return reviewsRepository.findAllByAuthorId(authorId, pageable);
    }

    public Page<Review> getReviewsByEntity(UUID entityId, Integer pageNumber, Integer pageSize) {
        if (pageSize > 50 || pageSize < 1) {
            pageSize = 50;
        }

        if (pageNumber < 1) {
            pageNumber = 1;
        }

        logger.debug(REVIEW_GET, "Fetching reviews by entity (entityId={}, page={}, size={})", entityId, pageNumber, pageSize);

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);

        return reviewsRepository.findAllByEntityId(entityId, pageable);
    }

    private void validateReview(Review review) {
        logger.debug(REVIEW_VALIDATION, "Validating review payload (authorId={}, entityId={})",
                review.getAuthorId(), review.getEntityId());

        if (review.getRating() < 1 || review.getRating() > 5) {
            logger.warn(REVIEW_VALIDATION, "Invalid rating: {}", review.getRating());
            throw new InvalidReviewUpdateException("Rating must be between 1 and 5");
        }
        if (review.getAuthorId() == null) {
            logger.warn(REVIEW_VALIDATION, "Missing authorId");
            throw new InvalidReviewUpdateException("Author ID is required");
        }
        if (review.getEntityId() == null) {
            logger.warn(REVIEW_VALIDATION, "Missing entityId");
            throw new InvalidReviewUpdateException("Entity ID is required");
        }
        if(review.getEntityType() == null) {
            logger.warn(REVIEW_VALIDATION, "Missing entityType");
            throw new InvalidReviewUpdateException("Entity Type is required");
        }
        if (review.getComment() == null || review.getComment().trim().isEmpty()) {
            logger.warn(REVIEW_VALIDATION, "Comment is empty or null");
            throw new InvalidReviewUpdateException("Comment cannot be empty");
        }
    }
}
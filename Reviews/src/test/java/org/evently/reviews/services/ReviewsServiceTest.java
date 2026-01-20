package org.evently.reviews.services;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.evently.reviews.clients.EventsClient;
import org.evently.reviews.clients.UsersClient;
import org.evently.reviews.enums.EntityType;
import org.evently.reviews.exceptions.InvalidReviewException;
import org.evently.reviews.exceptions.ReviewNotFoundException;
import org.evently.reviews.exceptions.externalServices.EventNotFoundException;
import org.evently.reviews.exceptions.externalServices.UserNotFoundException;
import org.evently.reviews.models.Review;
import org.evently.reviews.repositories.ReviewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewsServiceTest {

    @Mock private ReviewsRepository reviewsRepository;

    @Mock private EventsClient eventsClient;
    @Mock private UsersClient usersClient;

    @InjectMocks private ReviewsService reviewsService;

    private Review validReview;

    @BeforeEach
    void setup() {
        validReview = new Review();
        validReview.setId(null);
        validReview.setAuthorId(UUID.randomUUID());
        validReview.setEntityId(UUID.randomUUID());
        validReview.setEntityType(EntityType.EVENT);
        validReview.setRating(5);
        validReview.setComment("Top");
    }

    // getReview

    @Test
    void getReview_notFound_throwsReviewNotFoundException() {
        UUID id = UUID.randomUUID();
        when(reviewsRepository.findById(id)).thenReturn(Optional.empty());

        ReviewNotFoundException ex = assertThrows(ReviewNotFoundException.class,
                () -> reviewsService.getReview(id));

        assertEquals("Review not found", ex.getMessage());
    }

    @Test
    void getReview_success_returnsReview() {
        UUID id = UUID.randomUUID();
        Review r = new Review();
        r.setId(id);

        when(reviewsRepository.findById(id)).thenReturn(Optional.of(r));

        Review result = reviewsService.getReview(id);
        assertEquals(id, result.getId());
    }

    // deleteReview

    @Test
    void deleteReview_notFound_throwsReviewNotFoundException() {
        UUID id = UUID.randomUUID();
        when(reviewsRepository.existsById(id)).thenReturn(false);

        ReviewNotFoundException ex = assertThrows(ReviewNotFoundException.class,
                () -> reviewsService.deleteReview(id));

        assertEquals("Review not found", ex.getMessage());
        verify(reviewsRepository, never()).deleteById(any());
    }

    @Test
    void deleteReview_success_deletes() {
        UUID id = UUID.randomUUID();
        when(reviewsRepository.existsById(id)).thenReturn(true);

        reviewsService.deleteReview(id);

        verify(reviewsRepository).deleteById(id);
    }

    // getReviewsByAuthor (RAS001-004)

    @Test
    void getReviewsByAuthor_pageSizeGreaterThan50_adjustsTo50() {
        UUID authorId = UUID.randomUUID();
        int pageNumber = 1;
        int pageSize = 51;

        Page<Review> mockedPage = new PageImpl<>(Collections.singletonList(new Review()));
        when(reviewsRepository.findAllByAuthorId(eq(authorId), any(PageRequest.class)))
                .thenReturn(mockedPage);

        Page<Review> result = reviewsService.getReviewsByAuthor(authorId, pageNumber, pageSize);

        assertSame(mockedPage, result);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(reviewsRepository).findAllByAuthorId(eq(authorId), captor.capture());

        PageRequest used = captor.getValue();
        assertEquals(50, used.getPageSize());
        assertEquals(1, used.getPageNumber());
    }

    @Test
    void getReviewsByAuthor_pageSizeLessThan1_adjustsTo50() {
        UUID authorId = UUID.randomUUID();
        int pageNumber = 1;
        int pageSize = 0;

        Page<Review> mockedPage = new PageImpl<>(Collections.emptyList());
        when(reviewsRepository.findAllByAuthorId(eq(authorId), any(PageRequest.class)))
                .thenReturn(mockedPage);

        Page<Review> result = reviewsService.getReviewsByAuthor(authorId, pageNumber, pageSize);

        assertSame(mockedPage, result);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(reviewsRepository).findAllByAuthorId(eq(authorId), captor.capture());

        PageRequest used = captor.getValue();
        assertEquals(50, used.getPageSize());
        assertEquals(1, used.getPageNumber());
    }

    @Test
    void getReviewsByAuthor_pageNumberLessThan1_adjustsTo1() {
        UUID authorId = UUID.randomUUID();
        int pageNumber = 0;
        int pageSize = 10;

        Page<Review> mockedPage = new PageImpl<>(Arrays.asList(new Review(), new Review()));
        when(reviewsRepository.findAllByAuthorId(eq(authorId), any(PageRequest.class)))
                .thenReturn(mockedPage);

        Page<Review> result = reviewsService.getReviewsByAuthor(authorId, pageNumber, pageSize);

        assertSame(mockedPage, result);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(reviewsRepository).findAllByAuthorId(eq(authorId), captor.capture());

        PageRequest used = captor.getValue();
        assertEquals(10, used.getPageSize());
        assertEquals(1, used.getPageNumber());
    }

    @Test
    void getReviewsByAuthor_success_returnsPage() {
        UUID authorId = UUID.randomUUID();
        int pageNumber = 2;
        int pageSize = 20;

        Page<Review> mockedPage = new PageImpl<>(Collections.emptyList());
        when(reviewsRepository.findAllByAuthorId(eq(authorId), any(PageRequest.class)))
                .thenReturn(mockedPage);

        Page<Review> result = reviewsService.getReviewsByAuthor(authorId, pageNumber, pageSize);

        assertSame(mockedPage, result);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(reviewsRepository).findAllByAuthorId(eq(authorId), captor.capture());

        PageRequest used = captor.getValue();
        assertEquals(20, used.getPageSize());
        assertEquals(2, used.getPageNumber());
    }

    // registerReview

    @Test
    void registerReview_invalidRating_throwsInvalidReviewException() {
        validReview.setRating(0);

        InvalidReviewException ex = assertThrows(InvalidReviewException.class,
                () -> reviewsService.registerReview(validReview));

        assertEquals("Rating must be between 1 and 5", ex.getMessage());
        verifyNoInteractions(reviewsRepository);
    }

    @Test
    void registerReview_eventNotFound_throwsEventNotFoundException() {
        validReview.setEntityType(EntityType.EVENT);

        when(eventsClient.searchEvents(validReview.getEntityId()))
                .thenThrow(feignNotFound());

        EventNotFoundException ex = assertThrows(EventNotFoundException.class,
                () -> reviewsService.registerReview(validReview));

        assertEquals("(ReviewsService): Event not found in Events service", ex.getMessage());
        verify(reviewsRepository, never()).save(any());
    }

    @Test
    void registerReview_usersNotFound_throwsUserNotFoundException() {
        validReview.setEntityType(EntityType.EVENT);

        // evento existe
        when(eventsClient.searchEvents(validReview.getEntityId())).thenReturn(null);
        // user não existe
        when(usersClient.getUser(validReview.getAuthorId())).thenThrow(feignNotFound());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> reviewsService.registerReview(validReview));

        assertEquals("(ReviewsService): User not found in Users service", ex.getMessage());
        verify(reviewsRepository, never()).save(any());
    }

    @Test
    void registerReview_success_savesAndReturns() {
        validReview.setEntityType(EntityType.EVENT);

        when(eventsClient.searchEvents(validReview.getEntityId())).thenReturn(null);
        when(usersClient.getUser(validReview.getAuthorId())).thenReturn(null);

        when(reviewsRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        Review result = reviewsService.registerReview(validReview);

        assertNotNull(result.getId());
        assertEquals("Top", result.getComment());
        verify(reviewsRepository).save(any(Review.class));
    }

    private FeignException.NotFound feignNotFound() {
        Request req = Request.create(
                Request.HttpMethod.GET,
                "/fake",
                new HashMap<>(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );
        return new FeignException.NotFound("not found", req, null, null);
    }

}

package org.evently.reviews.services;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.evently.reviews.clients.UsersClient;
import org.evently.reviews.exceptions.ExternalServiceException;
import org.evently.reviews.exceptions.InvalidReviewCommentException;
import org.evently.reviews.exceptions.ReviewNotFoundException;
import org.evently.reviews.exceptions.externalServices.UserNotFoundException;
import org.evently.reviews.models.Review;
import org.evently.reviews.models.ReviewComment;
import org.evently.reviews.repositories.ReviewCommentsRepository;
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
class ReviewCommentsServiceTest {

    @Mock private ReviewCommentsRepository reviewCommentsRepository;
    @Mock private ReviewsRepository reviewsRepository;
    @Mock private UsersClient usersClient;

    @InjectMocks private ReviewCommentsService reviewCommentsService;

    private ReviewComment validComment;

    @BeforeEach
    void setup() {
        validComment = new ReviewComment();
        validComment.setId(null);
        validComment.setAuthorId(UUID.randomUUID());
        validComment.setComment("Nice!");
        Review r = new Review();
        r.setId(UUID.randomUUID());
        validComment.setReview(r);
    }

    // registerReviewComment

    @Test
    void registerReviewComment_authorIdNull_throwsInvalidReviewCommentException() {
        validComment.setAuthorId(null);

        InvalidReviewCommentException ex = assertThrows(InvalidReviewCommentException.class,
                () -> reviewCommentsService.registerReviewComment(validComment));

        assertEquals("Author ID is required", ex.getMessage());
        verifyNoInteractions(reviewCommentsRepository);
    }

    @Test
    void registerReviewComment_commentEmpty_throwsInvalidReviewCommentException() {
        validComment.setComment("  ");

        InvalidReviewCommentException ex = assertThrows(InvalidReviewCommentException.class,
                () -> reviewCommentsService.registerReviewComment(validComment));

        assertEquals("Comment text cannot be empty", ex.getMessage());
        verifyNoInteractions(reviewCommentsRepository);
    }

    @Test
    void registerReviewComment_reviewNull_throwsInvalidReviewCommentException() {
        validComment.setReview(null);

        InvalidReviewCommentException ex = assertThrows(InvalidReviewCommentException.class,
                () -> reviewCommentsService.registerReviewComment(validComment));

        assertEquals("A valid Review ID must be associated", ex.getMessage());
        verifyNoInteractions(reviewCommentsRepository);
    }

    @Test
    void registerReviewComment_userNotFound_throwsUserNotFoundException() {
        when(usersClient.getUser(validComment.getAuthorId())).thenThrow(feignNotFound());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> reviewCommentsService.registerReviewComment(validComment));

        assertEquals("(ReviewCommentsService): User not found in Users service", ex.getMessage());
        verify(reviewCommentsRepository, never()).save(any());
    }

    @Test
    void registerReviewComment_usersError_throwsExternalServiceException() {
        when(usersClient.getUser(validComment.getAuthorId()))
                .thenThrow(feignAnyError());

        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                () -> reviewCommentsService.registerReviewComment(validComment));

        assertEquals("(ReviewCommentsService): Users service error", ex.getMessage());
        verify(reviewCommentsRepository, never()).save(any());
    }

    @Test
    void registerReviewComment_parentReviewNotFound_throwsReviewNotFoundException() {
        when(usersClient.getUser(validComment.getAuthorId())).thenReturn(null);
        when(reviewsRepository.existsById(validComment.getReview().getId())).thenReturn(false);

        ReviewNotFoundException ex = assertThrows(ReviewNotFoundException.class,
                () -> reviewCommentsService.registerReviewComment(validComment));

        assertEquals("Review not found", ex.getMessage());
        verify(reviewCommentsRepository, never()).save(any());
    }

    @Test
    void registerReviewComment_success_savesAndReturns() {
        when(usersClient.getUser(validComment.getAuthorId())).thenReturn(null);
        when(reviewsRepository.existsById(validComment.getReview().getId())).thenReturn(true);

        when(reviewCommentsRepository.save(any(ReviewComment.class))).thenAnswer(inv -> {
            ReviewComment c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ReviewComment result = reviewCommentsService.registerReviewComment(validComment);

        assertNotNull(result.getId());
        assertEquals("Nice!", result.getComment());
        verify(reviewCommentsRepository).save(any(ReviewComment.class));
    }

    // getReviewCommentsByReview (CLS...)

    @Test
    void getReviewCommentsByReview_pageSizeGreaterThan50_adjustsTo50() {
        Review review = new Review();
        review.setId(UUID.randomUUID());

        Page<ReviewComment> mockedPage = new PageImpl<>(Collections.singletonList(new ReviewComment()));
        when(reviewCommentsRepository.findAllByReview(eq(review), any(PageRequest.class)))
                .thenReturn(mockedPage);

        Page<ReviewComment> result = reviewCommentsService.getReviewCommentsByReview(review, 1, 51);

        assertSame(mockedPage, result);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(reviewCommentsRepository).findAllByReview(eq(review), captor.capture());

        PageRequest used = captor.getValue();
        assertEquals(50, used.getPageSize());
        assertEquals(1, used.getPageNumber());
    }

    @Test
    void getReviewCommentsByReview_pageNumberLessThan1_adjustsTo1() {
        Review review = new Review();
        review.setId(UUID.randomUUID());

        Page<ReviewComment> mockedPage = new PageImpl<>(Collections.emptyList());
        when(reviewCommentsRepository.findAllByReview(eq(review), any(PageRequest.class)))
                .thenReturn(mockedPage);

        Page<ReviewComment> result = reviewCommentsService.getReviewCommentsByReview(review, 0, 10);

        assertSame(mockedPage, result);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(reviewCommentsRepository).findAllByReview(eq(review), captor.capture());

        PageRequest used = captor.getValue();
        assertEquals(10, used.getPageSize());
        assertEquals(1, used.getPageNumber());
    }

    // ---- helpers para FeignException ----
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

    private FeignException feignAnyError() {
        Request req = Request.create(
                Request.HttpMethod.GET,
                "/fake",
                new HashMap<>(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );
        return new FeignException.InternalServerError("err", req, null, null);
    }

}

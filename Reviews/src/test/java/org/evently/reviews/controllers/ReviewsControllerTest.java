package org.evently.reviews.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evently.reviews.dtos.reviews.ReviewCreateDTO;
import org.evently.reviews.enums.EntityType;
import org.evently.reviews.exceptions.ReviewNotFoundException;
import org.evently.reviews.models.Review;
import org.evently.reviews.services.ReviewsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ReviewsController.class)
class ReviewsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ReviewsService reviewsService;

    @Test
    void getReview_success_returns200AndDto() throws Exception {
        UUID id = UUID.randomUUID();

        Review review = new Review();
        review.setId(id);
        review.setAuthorId(UUID.randomUUID());
        review.setEntityId(UUID.randomUUID());
        review.setEntityType(EntityType.EVENT);
        review.setRating(5);
        review.setComment("Top!");
        review.setCreatedAt(new Date());

        when(reviewsService.getReview(id)).thenReturn(review);

        mockMvc.perform(get("/reviews/get-review/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Top!"));
    }

    @Test
    void getReview_notFound_returns404AndMessage() throws Exception {
        UUID id = UUID.randomUUID();
        when(reviewsService.getReview(id)).thenThrow(new ReviewNotFoundException("Review not found"));

        mockMvc.perform(get("/reviews/get-review/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Review not found"));
    }

    @Test
    void getReview_genericError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(reviewsService.getReview(id)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/reviews/get-review/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

    @Test
    void getReviewsByAuthor_success_returns200AndPage() throws Exception {
        UUID authorId = UUID.randomUUID();

        Review r1 = new Review(); r1.setId(UUID.randomUUID()); r1.setRating(4); r1.setComment("Ok");
        Review r2 = new Review(); r2.setId(UUID.randomUUID()); r2.setRating(5); r2.setComment("Great");

        Page<Review> page = new PageImpl<>(Arrays.asList(r1, r2));
        when(reviewsService.getReviewsByAuthor(eq(authorId), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/reviews/author/{authorId}/{pageNumber}/{pageSize}", authorId, 1, 10)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].comment").value("Ok"));
    }

    @Test
    void getReviewsByEntity_success_returns200AndPage() throws Exception {
        UUID entityId = UUID.randomUUID();

        Review r1 = new Review(); r1.setId(UUID.randomUUID()); r1.setRating(3); r1.setComment("meh");
        Page<Review> page = new PageImpl<>(Collections.singletonList(r1));

        when(reviewsService.getReviewsByEntity(eq(entityId), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/reviews/entity/{entityId}/{pageNumber}/{pageSize}", entityId, 1, 10)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].comment").value("meh"));
    }

    @Test
    void registerReview_success_returns201AndDto() throws Exception {
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setAuthorId(UUID.randomUUID());
        dto.setEntityId(UUID.randomUUID());
        dto.setEntityType(EntityType.EVENT);
        dto.setRating(5);
        dto.setComment("Nice");

        Review saved = new Review();
        saved.setId(UUID.randomUUID());
        saved.setRating(5);
        saved.setComment("Nice");
        saved.setAuthorId(dto.getAuthorId());
        saved.setEntityId(dto.getEntityId());
        saved.setEntityType(dto.getEntityType());

        when(reviewsService.registerReview(any(Review.class))).thenReturn(saved);

        mockMvc.perform(post("/reviews/register-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.comment").value("Nice"))
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void registerReview_error_returns400AndMessage() throws Exception {
        ReviewCreateDTO dto = new ReviewCreateDTO();
        dto.setAuthorId(UUID.randomUUID());
        dto.setEntityId(UUID.randomUUID());
        dto.setEntityType(EntityType.EVENT);
        dto.setRating(0);
        dto.setComment("x");

        when(reviewsService.registerReview(any(Review.class)))
                .thenThrow(new RuntimeException("Rating must be between 1 and 5"));

        mockMvc.perform(post("/reviews/register-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Rating must be between 1 and 5"));
    }

    @Test
    void deleteReview_success_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/reviews/delete-review/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReview_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ReviewNotFoundException("Review not found"))
                .when(reviewsService).deleteReview(id);

        mockMvc.perform(delete("/reviews/delete-review/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Review not found"));
    }

    @Test
    void deleteReview_genericError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("boom"))
                .when(reviewsService).deleteReview(id);

        mockMvc.perform(delete("/reviews/delete-review/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }
}

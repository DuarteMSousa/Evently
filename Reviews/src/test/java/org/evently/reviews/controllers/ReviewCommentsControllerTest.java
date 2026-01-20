package org.evently.reviews.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evently.reviews.dtos.reviewComments.ReviewCommentCreateDTO;
import org.evently.reviews.exceptions.ReviewCommentNotFoundException;
import org.evently.reviews.models.ReviewComment;
import org.evently.reviews.services.ReviewCommentsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ReviewCommentsController.class)
class ReviewCommentsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ReviewCommentsService reviewCommentsService;

    @Test
    void getComment_success_returns200AndDto() throws Exception {
        UUID id = UUID.randomUUID();

        ReviewComment c = new ReviewComment();
        c.setId(id);
        c.setAuthorId(UUID.randomUUID());
        c.setComment("hello");
        c.setCreatedAt(new Date());

        when(reviewCommentsService.getReviewComment(id)).thenReturn(c);

        mockMvc.perform(get("/reviews/comments/get-comment/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.comment").value("hello"));
    }

    @Test
    void getComment_notFound_returns404AndMessage() throws Exception {
        UUID id = UUID.randomUUID();
        when(reviewCommentsService.getReviewComment(id))
                .thenThrow(new ReviewCommentNotFoundException("Review comment not found"));

        mockMvc.perform(get("/reviews/comments/get-comment/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Review comment not found"));
    }

    @Test
    void getComment_genericError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(reviewCommentsService.getReviewComment(id)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/reviews/comments/get-comment/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

    @Test
    void getCommentsByReview_success_returns200AndPage() throws Exception {
        UUID reviewId = UUID.randomUUID();

        ReviewComment c1 = new ReviewComment(); c1.setId(UUID.randomUUID()); c1.setComment("c1");
        ReviewComment c2 = new ReviewComment(); c2.setId(UUID.randomUUID()); c2.setComment("c2");

        Page<ReviewComment> page = new PageImpl<>(Arrays.asList(c1, c2));
        when(reviewCommentsService.getReviewCommentsByReview(any(), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/reviews/comments/review/{reviewId}/{pageNumber}/{pageSize}", reviewId, 1, 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].comment").value("c1"));
    }

    @Test
    void registerComment_success_returns201AndDto() throws Exception {
        ReviewCommentCreateDTO dto = new ReviewCommentCreateDTO();
        dto.setAuthor(UUID.randomUUID());
        dto.setReviewId(UUID.randomUUID());
        dto.setComment("nice");

        ReviewComment saved = new ReviewComment();
        saved.setId(UUID.randomUUID());
        saved.setAuthorId(dto.getAuthor());
        saved.setComment("nice");
        saved.setCreatedAt(new Date());

        when(reviewCommentsService.registerReviewComment(any(ReviewComment.class))).thenReturn(saved);

        mockMvc.perform(post("/reviews/comments/register-comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.comment").value("nice"));
    }

    @Test
    void registerComment_error_returns400AndMessage() throws Exception {
        ReviewCommentCreateDTO dto = new ReviewCommentCreateDTO();
        dto.setAuthor(UUID.randomUUID());
        dto.setReviewId(UUID.randomUUID());
        dto.setComment("");

        when(reviewCommentsService.registerReviewComment(any(ReviewComment.class)))
                .thenThrow(new RuntimeException("Comment text cannot be empty"));

        mockMvc.perform(post("/reviews/comments/register-comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Comment text cannot be empty"));
    }

    @Test
    void deleteComment_success_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/reviews/comments/delete-comment/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteComment_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ReviewCommentNotFoundException("Review Comment not found"))
                .when(reviewCommentsService).deleteReviewComment(id);


        mockMvc.perform(delete("/reviews/comments/delete-comment/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Review Comment not found"));
    }

    @Test
    void deleteComment_genericError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("boom"))
                .when(reviewCommentsService).deleteReviewComment(id);


        mockMvc.perform(delete("/reviews/comments/delete-comment/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

}

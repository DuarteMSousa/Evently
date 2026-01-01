package org.example.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.exceptions.CategoryNotFoundException;
import org.example.models.Category;
import org.example.services.CategoriesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoriesController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoriesControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CategoriesService categoriesService;

    @Test
    void getCategories_success_returns200() throws Exception {
        Category c1 = new Category(); c1.setId(UUID.randomUUID()); c1.setName("A");
        Category c2 = new Category(); c2.setId(UUID.randomUUID()); c2.setName("B");

        when(categoriesService.getCategories()).thenReturn(Arrays.asList(c1, c2));

        mockMvc.perform(get("/events/categories/get-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("A"));
    }

    @Test
    void getCategory_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(categoriesService.getCategory(id)).thenThrow(new CategoryNotFoundException("Category not found"));

        mockMvc.perform(get("/events/categories/get-category/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Category not found"));
    }

    @Test
    void deleteCategory_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/events/categories/delete-category/{id}", id))
                .andExpect(status().isOk());

        verify(categoriesService).deleteCategory(id);
    }

    // create/update -> assumem @RequestBody no controller
    @Test
    void createCategory_success_returns200() throws Exception {
        String body = "{\"name\":\"Music\"}";

        Category created = new Category();
        created.setId(UUID.randomUUID());
        created.setName("Music");

        when(categoriesService.createCategory(any(Category.class))).thenReturn(created);

        mockMvc.perform(post("/events/categories/create-category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Music"));
    }

    @Test
    void updateCategory_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"id\":\"" + id + "\",\"name\":\"New\"}";

        Category updated = new Category();
        updated.setId(id);
        updated.setName("New");

        when(categoriesService.updateCategory(eq(id), any(Category.class))).thenReturn(updated);

        mockMvc.perform(post("/events/categories/update-category/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"));
    }
}

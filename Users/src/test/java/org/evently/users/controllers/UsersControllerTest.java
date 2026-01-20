package org.evently.users.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evently.users.dtos.User.*;
import org.evently.users.exceptions.*;
import org.evently.users.models.User;
import org.evently.users.services.UsersService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UsersController.class)
class UsersControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UsersService usersService;

    // ---------- get-user ----------

    @Test
    void getUser_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        User u = new User();
        u.setId(id);
        u.setUsername("ana");

        when(usersService.getUser(id)).thenReturn(u);

        mockMvc.perform(get("/users/get-user/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.username").value("ana"));
    }

    @Test
    void getUser_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(usersService.getUser(id)).thenThrow(new UserNotFoundException(""));

        mockMvc.perform(get("/users/get-user/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUser_genericError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(usersService.getUser(id)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/users/get-user/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

    // ---------- create-user ----------

    @Test
    void createUser_success_returns201() throws Exception {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("ana");
        dto.setPassword("123");
        dto.setEmail("a@a.com");

        User saved = new User();
        saved.setId(UUID.randomUUID());
        saved.setUsername("ana");

        when(usersService.createUser(any(User.class))).thenReturn(saved);

        mockMvc.perform(post("/users/create-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ana"));
    }

    @Test
    void createUser_duplicate_returns404() throws Exception {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("ana");

        when(usersService.createUser(any(User.class)))
                .thenThrow(new UserAlreadyExistsException("dup"));

        mockMvc.perform(post("/users/create-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("dup"));
    }

    // ---------- update-user ----------

    @Test
    void updateUser_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setId(id);
        dto.setUsername("ana");

        User updated = new User();
        updated.setId(id);
        updated.setUsername("ana");

        when(usersService.updateUser(eq(id), any(User.class))).thenReturn(updated);

        mockMvc.perform(put("/users/update-user/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana"));
    }

    @Test
    void updateUser_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setId(id);

        when(usersService.updateUser(eq(id), any(User.class)))
                .thenThrow(new UserNotFoundException("not found"));

        mockMvc.perform(put("/users/update-user/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ---------- deactivate-user ----------

    @Test
    void deactivateUser_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        User u = new User(); u.setId(id); u.setActive(false);

        when(usersService.deactivateUser(id)).thenReturn(u);

        mockMvc.perform(put("/users/deactivate-user/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deactivateUser_alreadyDeactivated_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(usersService.deactivateUser(id))
                .thenThrow(new UserAlreadyDeactivatedException("already"));

        mockMvc.perform(put("/users/deactivate-user/{id}", id))
                .andExpect(status().isConflict());
    }

    // ---------- get-users-page ----------

    @Test
    void getUsersPage_success_returns200() throws Exception {
        Page<User> emptyPage = new PageImpl<>(Collections.emptyList());
        when(usersService.getUsersPage(1, 10)).thenReturn(emptyPage);

        mockMvc.perform(get("/users/get-users/{p}/{s}", 1, 10))
                .andExpect(status().isOk());
    }


    // ---------- login ----------

    @Test
    void login_success_returns200() throws Exception {
        UserLoginRequestDTO dto = new UserLoginRequestDTO();
        dto.setUsername("ana");
        dto.setPassword("123");

        when(usersService.loginUser("ana", "123")).thenReturn("TOKEN");

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("TOKEN"));
    }

    @Test
    void login_failed_returns401() throws Exception {
        UserLoginRequestDTO dto = new UserLoginRequestDTO();
        dto.setUsername("ana"); dto.setPassword("123");

        when(usersService.loginUser("ana", "123"))
                .thenThrow(new LoginFailedException("fail"));

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

}

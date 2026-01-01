package org.evently.users.services;

import org.evently.users.exceptions.*;
import org.evently.users.models.User;
import org.evently.users.repositories.UsersRepository;
import org.evently.users.Utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock private UsersRepository usersRepository;
    @Mock private JwtUtils jwtUtils;
    @InjectMocks private UsersService usersService;

    private User validUser;

    @BeforeEach
    void setup() {
        validUser = new User();
        validUser.setId(UUID.randomUUID());
        validUser.setUsername("ana");
        validUser.setEmail("a@a.com");
        validUser.setNif("123");
        validUser.setPhoneNumber("999");
        validUser.setPassword("123");
        validUser.setActive(true);
    }

    // createUser
    @Test void createUser_duplicateUsername_throws() {
        when(usersRepository.existsByUsername("ana")).thenReturn(true);
        assertThrows(UserAlreadyExistsException.class, () -> usersService.createUser(validUser));
    }

    @Test void createUser_success_saves() {
        when(usersRepository.existsByUsername("ana")).thenReturn(false);
        when(usersRepository.existsByEmail("a@a.com")).thenReturn(false);
        when(usersRepository.existsByNif("123")).thenReturn(false);
        when(usersRepository.existsByPhoneNumber("999")).thenReturn(false);
        when(usersRepository.save(any())).thenReturn(validUser);

        User res = usersService.createUser(validUser);
        assertNotNull(res);
    }

    // getUser
    @Test void getUser_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(usersRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> usersService.getUser(id));
    }

    // deactivateUser
    @Test void deactivateUser_alreadyDeactivated_throws() {
        UUID id = UUID.randomUUID();
        User u = new User(); u.setId(id); u.setActive(false);
        when(usersRepository.findById(id)).thenReturn(Optional.of(u));
        assertThrows(UserAlreadyDeactivatedException.class, () -> usersService.deactivateUser(id));
    }

    // loginUser
    @Test void loginUser_invalidPassword_throws() {
        when(usersRepository.findByUsername("ana")).thenReturn(Optional.of(validUser));
        assertThrows(LoginFailedException.class, () -> usersService.loginUser("ana", "wrong"));
    }

    // getUsersPage
    @Test void getUsersPage_pageSizeGreaterThan50_adjusts() {
        usersService.getUsersPage(1, 100);
        verify(usersRepository).findAll(PageRequest.of(1, 50));
    }
}

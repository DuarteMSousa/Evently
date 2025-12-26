package org.evently.users.controllers;

import org.evently.users.dtos.User.*;
import org.evently.users.exceptions.LoginFailedException;
import org.evently.users.exceptions.UserAlreadyDeactivatedException;
import org.evently.users.exceptions.UserAlreadyExistsException;
import org.evently.users.exceptions.UserNotFoundException;
import org.evently.users.models.User;
import org.evently.users.services.UsersService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UsersController {

    @Autowired
    private UsersService usersService;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(UsersController.class);

    private Marker marker = MarkerFactory.getMarker("UsersController");


    @GetMapping("/get-user/{id}")
    public ResponseEntity<?> getUser(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - undefined error
         * 404 NOT_FOUND - user not found
         */
        logger.info(marker, "Method get user entered");
        User user;

        try {
            user = usersService.getUser(id);
        } catch (UserNotFoundException e) {
            logger.error(marker, "UserNotFoundException caught while getting user {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while getting user {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, user found");
        return ResponseEntity.status(HttpStatus.OK).body(
                modelMapper.map(user, UserDTO.class)
        );

    }

    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(@RequestBody UserCreateDTO userCreateDTO) {
        /* HttpStatus(produces)
         * 201 CREATED - Request processed as expected.
         * 400 INTERNAL_SERVER_ERROR - undefined error
         * 404 NOT_FOUND - user not found
         */
        logger.info(marker, "Method create user entered");
        User newUser;

        try {
            newUser = usersService.createUser(modelMapper.map(userCreateDTO, User.class));
        } catch (UserAlreadyExistsException e) {
            logger.error(marker, "UserAlreadyExistsException caught while creating user");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while creating user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, user created");
        return ResponseEntity.status(HttpStatus.CREATED).body(
                modelMapper.map(newUser, UserDTO.class)
        );

    }

    @PutMapping("/update-user/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") UUID id, @RequestBody UserUpdateDTO userUpdateDTO) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - undefined error
         * 404 NOT_FOUND - user not found
         */
        logger.info(marker, "Method update user entered");
        User updatedUser;

        try {
            updatedUser = usersService.updateUser(id, modelMapper.map(userUpdateDTO, User.class));
        } catch (UserNotFoundException e) {
            logger.error(marker, "UserNotFoundException caught while updating user");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while updating user {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, user updated");
        return ResponseEntity.status(HttpStatus.OK).body(
                modelMapper.map(updatedUser, UserDTO.class)
        );
    }

    @PutMapping("/deactivate-user/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - undefined error
         * 409 CONFLICT - user already deactivated
         * 404 NOT_FOUND - user not found
         */
        logger.info(marker, "Method deactivate user entered");
        User updatedUser;

        try {
            updatedUser = usersService.deactivateUser(id);
        } catch (UserNotFoundException e) {
            logger.error(marker, "UserNotFoundException caught while deactivating user");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UserAlreadyDeactivatedException e) {
            logger.error(marker, "UserAlreadyDeactivatedException caught while deactivating user");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while deactivating user {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, user deactivated");
        return ResponseEntity.status(HttpStatus.OK).body(modelMapper.map(updatedUser, UserDTO.class));
    }

    @GetMapping("/get-users/{pageNumber}/{pageSize}")
    public ResponseEntity<?> getUsersPage(@PathVariable("pageNumber") Integer pageNumber, @PathVariable("pageSize") Integer pageSize) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - undefined error
         */
        logger.info(marker, "Method getUsersPage entered");
        Page<UserDTO> usersPage;

        try {
            usersPage = usersService.getUsersPage(pageNumber, pageSize)
                    .map(user -> modelMapper.map(user, UserDTO.class));
        } catch (Exception e) {
            logger.error(marker, "Exception caught while getting users page {}",e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, users page found");
        return ResponseEntity.status(HttpStatus.OK).body(usersPage);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequestDTO dto) {
        logger.info(marker, "Method login entered");
        try {
            String token = usersService.loginUser(dto.getUsername(), dto.getPassword());
            logger.info(marker, "200 OK returned, user logged in");
            return ResponseEntity.ok(new UserLoginResponseDTO(token));
        } catch (LoginFailedException e) {
            logger.error(marker, "LoginFailedException caught while logging in");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error(marker, "Exception caught while logging in user {}: {}", dto.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

}

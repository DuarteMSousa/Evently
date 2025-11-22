package org.evently.users.controllers;

import org.evently.users.dtos.User.UserCreateDTO;
import org.evently.users.dtos.User.UserDTO;
import org.evently.users.dtos.User.UserUpdateDTO;
import org.evently.users.exceptions.UserAlreadyDeactivatedException;
import org.evently.users.exceptions.UserNotFoundException;
import org.evently.users.models.User;
import org.evently.users.services.UsersService;
import org.modelmapper.ModelMapper;
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

    ModelMapper modelMapper;

    UsersController() {
        modelMapper = new ModelMapper();
    }

    @GetMapping("/get-user/{id}")
    public ResponseEntity<?> getUser(@PathVariable("id") UUID id) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - undefined error
         * 404 NOT_FOUND - user not found
         */

        User user;

        try {
            user = usersService.getUser(id);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

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

        User newUser;

        try {
            newUser = usersService.createUser(modelMapper.map(userCreateDTO, User.class));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

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

        User updatedUser;

        try {
            updatedUser = usersService.updateUser(id, modelMapper.map(userUpdateDTO, User.class));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

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

        User updatedUser;

        try {
            updatedUser = usersService.deactivateUser(id);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UserAlreadyDeactivatedException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.OK).body(modelMapper.map(updatedUser, UserDTO.class));
    }

    @GetMapping("/get-users/{pageNumber}/{pageSize}")
    public ResponseEntity<?> getUsersPage(@PathVariable("pageNumber") Integer pageNumber, @PathVariable("pageSize") Integer pageSize) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - undefined error
         */
        Page<UserDTO> usersPage;

        try {
            usersPage = usersService.getUsersPage(pageNumber, pageSize)
                    .map(user -> modelMapper.map(user, UserDTO.class));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.OK).body(usersPage);
    }
}

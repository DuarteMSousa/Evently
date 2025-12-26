package org.evently.users.services;

import org.evently.users.Utils.JwtUtils;
import org.evently.users.Utils.PasswordUtils;
import org.evently.users.exceptions.*;
import org.evently.users.models.User;
import org.evently.users.repositories.UsersRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import java.awt.print.Pageable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UsersService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(UsersService.class);

    private Marker marker = MarkerFactory.getMarker("UsersService");

    @Transactional
    public User createUser(User user) {
        logger.info(marker, "Create user method entered");
        if (usersRepository.existsByUsername(user.getUsername())) {
            logger.error(marker, "Username {} already exists", user.getUsername());
            throw new UserAlreadyExistsException("User with username " + user.getUsername() + " already exists");
        }

        if (usersRepository.existsByEmail(user.getEmail())) {
            logger.error(marker, "Email {} already exists", user.getEmail());
            throw new UserAlreadyExistsException("User with email " + user.getEmail() + " already exists");
        }

        if (usersRepository.existsByNif(user.getNif())) {
            logger.error(marker, "Nif {} already exists", user.getNif());
            throw new UserAlreadyExistsException("User with nif " + user.getNif() + " already exists");
        }

        if (usersRepository.existsByPhoneNumber(user.getPhoneNumber())) {
            logger.error(marker, "Phone number {} already exists", user.getPhoneNumber());
            throw new UserAlreadyExistsException("User with phone number " + user.getNif() + " already exists");
        }

        user.setPassword(PasswordUtils.hashPassword(user.getPassword()));

        return usersRepository.save(user);
    }

    @Transactional
    public User updateUser(UUID id, User user) {
        logger.info(marker, "Update user method entered");
        if (!id.equals(user.getId())) {
            logger.error(marker, "Parameter id and body id do not correspond, user update failed");
            throw new InvalidUserUpdateException("Parameter id and body id do not correspond");
        }

        User existingUser = usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.getUsername().equals(existingUser.getUsername()) && usersRepository.existsByUsername(user.getUsername())) {
            logger.error(marker, "Updated Username {} already exists", user.getUsername());
            throw new UserAlreadyExistsException("User with username " + user.getUsername() + " already exists");
        }

        if (!user.getEmail().equals(existingUser.getEmail()) && usersRepository.existsByEmail(user.getEmail())) {
            logger.error(marker, "Updated Email {} already exists", user.getEmail());
            throw new UserAlreadyExistsException("User with email " + user.getEmail() + " already exists");
        }

        if (!user.getNif().equals(existingUser.getNif()) && usersRepository.existsByNif(user.getNif())) {
            logger.error(marker, "Updated Nif {} already exists", user.getNif());
            throw new UserAlreadyExistsException("User with nif " + user.getNif() + " already exists");
        }

        if (!user.getPhoneNumber().equals(existingUser.getPhoneNumber()) && usersRepository.existsByPhoneNumber(user.getPhoneNumber())) {
            logger.error(marker, "Updated Phone number {} already exists", user.getPhoneNumber());
            throw new UserAlreadyExistsException("User with phone number " + user.getNif() + " already exists");
        }

        existingUser.setUsername(user.getUsername());
        existingUser.setPassword(PasswordUtils.hashPassword(user.getPassword()));
        existingUser.setEmail(user.getEmail());
        existingUser.setBirthDate(user.getBirthDate());
        existingUser.setNif(user.getNif());
        existingUser.setPhoneNumber(user.getPhoneNumber());

        return usersRepository.save(existingUser);
    }

    public User getUser(UUID userId) {
        logger.info(marker, "Get user method entered");
        return usersRepository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(""));
    }

    @Transactional
    public User deactivateUser(UUID userId) {
        logger.info(marker, "Deactivate user method entered");
        User userToDeactivate = usersRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(""));

        if (!userToDeactivate.isActive()) {
            logger.error(marker, "User already deactivated found");
            throw new UserAlreadyDeactivatedException("");
        }

        userToDeactivate.setActive(false);
        return usersRepository.save(userToDeactivate);
    }

    public String loginUser(String username, String password) {
        logger.info(marker, "Login user method entered");
        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new LoginFailedException("User not found"));

        if (!PasswordUtils.checkPassword(password, user.getPassword())) {
            logger.error(marker, "Invalid username or password");
            throw new LoginFailedException("Invalid credentials");
        }

        return jwtUtils.generateToken(user.getId(), user.getUsername());
    }

    public Page<User> getUsersPage(Integer pageNumber, Integer pageSize) {
        logger.info(marker, "Get users page method entered");
        if (pageSize > 50) {
            pageSize = 50;
        }
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return usersRepository.findAll(pageable);
    }
}

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
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import java.util.UUID;

@Service
public class UsersService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private Logger logger = LoggerFactory.getLogger(UsersService.class);

    private static final Marker USER_LOGIN = MarkerFactory.getMarker("USER_LOGIN");
    private static final Marker USERS_GET = MarkerFactory.getMarker("USERS_GET");
    private static final Marker USER_GET = MarkerFactory.getMarker("USER_GET");
    private static final Marker USER_DEACTIVATE = MarkerFactory.getMarker("USER_DEACTIVATE");
    private static final Marker USER_UPDATE = MarkerFactory.getMarker("USER_UPDATE");
    private static final Marker USER_CREATE = MarkerFactory.getMarker("USER_CREATE");

    /**
     * Creates a new user after validating uniqueness constraints and hashing the password.
     *
     *
     * @param user user payload to create (must include username, email, nif, phoneNumber, password, etc.)
     * @return persisted user
     *
     * @throws UserAlreadyExistsException if any uniqueness constraint is violated (username/email/nif/phoneNumber)
     */
    @Transactional
    public User createUser(User user) {
        logger.info(USER_CREATE, "Create user method entered");

        if (usersRepository.existsByUsername(user.getUsername())) {
            logger.error(USER_CREATE, "Username {} already exists", user.getUsername());
            throw new UserAlreadyExistsException("User with username " + user.getUsername() + " already exists");
        }

        if (usersRepository.existsByEmail(user.getEmail())) {
            logger.error(USER_CREATE, "Email {} already exists", user.getEmail());
            throw new UserAlreadyExistsException("User with email " + user.getEmail() + " already exists");
        }

        if (usersRepository.existsByNif(user.getNif())) {
            logger.error(USER_CREATE, "Nif {} already exists", user.getNif());
            throw new UserAlreadyExistsException("User with nif " + user.getNif() + " already exists");
        }

        if (usersRepository.existsByPhoneNumber(user.getPhoneNumber())) {
            logger.error(USER_CREATE, "Phone number {} already exists", user.getPhoneNumber());
            throw new UserAlreadyExistsException("User with phone number " + user.getNif() + " already exists");
        }

        user.setPassword(PasswordUtils.hashPassword(user.getPassword()));
        return usersRepository.save(user);
    }

    /**
     * Updates an existing user.
     *
     *
     * @param id   user identifier from the request path
     * @param user user payload containing updated fields (must include id)
     * @return updated persisted user
     *
     * @throws InvalidUserUpdateException  if path id and body id do not match
     * @throws UserNotFoundException       if the user does not exist
     * @throws UserAlreadyExistsException  if any uniqueness constraint is violated after changes
     */
    @Transactional
    public User updateUser(UUID id, User user) {
        logger.info(USER_UPDATE, "Update user method entered");

        if (!id.equals(user.getId())) {
            logger.error(USER_UPDATE, "Parameter id and body id do not correspond, user update failed");
            throw new InvalidUserUpdateException("Parameter id and body id do not correspond");
        }

        User existingUser = usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.getUsername().equals(existingUser.getUsername())
                && usersRepository.existsByUsername(user.getUsername())) {
            logger.error(USER_UPDATE, "Updated Username {} already exists", user.getUsername());
            throw new UserAlreadyExistsException("User with username " + user.getUsername() + " already exists");
        }

        if (!user.getEmail().equals(existingUser.getEmail())
                && usersRepository.existsByEmail(user.getEmail())) {
            logger.error(USER_UPDATE, "Updated Email {} already exists", user.getEmail());
            throw new UserAlreadyExistsException("User with email " + user.getEmail() + " already exists");
        }

        if (!user.getNif().equals(existingUser.getNif())
                && usersRepository.existsByNif(user.getNif())) {
            logger.error(USER_UPDATE, "Updated Nif {} already exists", user.getNif());
            throw new UserAlreadyExistsException("User with nif " + user.getNif() + " already exists");
        }

        if (!user.getPhoneNumber().equals(existingUser.getPhoneNumber())
                && usersRepository.existsByPhoneNumber(user.getPhoneNumber())) {
            logger.error(USER_UPDATE, "Updated Phone number {} already exists", user.getPhoneNumber());
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

    /**
     * Retrieves a user by its identifier.
     *
     * @param userId user identifier
     * @return found user
     *
     * @throws UserNotFoundException if no user exists with the given id
     */
    public User getUser(UUID userId) {
        logger.info(USER_GET, "Get user method entered");

        return usersRepository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(""));
    }

    /**
     * Deactivates (soft-deletes) a user by setting active=false.
     *
     *
     * @param userId user identifier
     * @return updated user with active=false
     *
     * @throws UserNotFoundException             if the user does not exist
     * @throws UserAlreadyDeactivatedException   if the user is already deactivated
     */
    @Transactional
    public User deactivateUser(UUID userId) {
        logger.info(USER_DEACTIVATE, "Deactivate user method entered");

        User userToDeactivate = usersRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(""));

        if (!userToDeactivate.isActive()) {
            logger.error(USER_DEACTIVATE, "User already deactivated");
            throw new UserAlreadyDeactivatedException("User already deactivated");
        }

        userToDeactivate.setActive(false);
        return usersRepository.save(userToDeactivate);
    }

    /**
     * Authenticates a user using username/password and returns a JWT token if valid.
     *
     *
     * @param username username to authenticate
     * @param password raw password provided by the user
     * @return JWT token as a String
     *
     * @throws LoginFailedException if the user does not exist or the password is invalid
     */
    public String loginUser(String username, String password) {
        logger.info(USER_LOGIN, "Login user method entered");

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new LoginFailedException("User not found"));

        if (!PasswordUtils.checkPassword(password, user.getPassword())) {
            logger.error(USER_LOGIN, "Invalid username or password");
            throw new LoginFailedException("Invalid credentials");
        }

        return jwtUtils.generateToken(user.getId(), user.getUsername());
    }

    /**
     * Retrieves a paginated list of users.
     *
     *
     * @param pageNumber page index (0-based)
     * @param pageSize   number of elements per page (clamped to max 50)
     * @return a {@link Page} of users
     */
    public Page<User> getUsersPage(Integer pageNumber, Integer pageSize) {
        logger.info(USERS_GET, "Get users page method entered");

        if (pageSize > 50 || pageSize <= 0) {
            pageSize = 50;
        }

        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        return usersRepository.findAll(pageable);
    }

}

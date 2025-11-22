package org.evently.users.services;

import org.evently.users.Utils.PasswordUtils;
import org.evently.users.exceptions.LoginFailedException;
import org.evently.users.exceptions.UserAlreadyExistsException;
import org.evently.users.exceptions.UserNotFoundException;
import org.evently.users.models.User;
import org.evently.users.repositories.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UsersService {

    @Autowired
    private UsersRepository usersRepository;

    @Transactional
    public User createUser(User user) {
        if (usersRepository.existsByUsername(user.getUsername())) {
            throw new UserAlreadyExistsException("User with username " + user.getUsername() + " already exists");
        }

        user.setPassword(PasswordUtils.hashPassword(user.getPassword()));

        return usersRepository.save(user);
    }

    @Transactional
    public User updateUser(User user) {
        if (!usersRepository.existsById(user.getId())) {
            throw new UserNotFoundException("User not found");
        }

        user.setPassword(PasswordUtils.hashPassword(user.getPassword()));

        return usersRepository.save(user);
    }

    public User getUser(UUID userId) {
        return usersRepository
                .findById(userId)
                .orElseThrow(()-> new UserNotFoundException(""));
    }

    public List<User> getUsersPage() {
        return new ArrayList<User>();
    }

    @Transactional
    public User deactivateUser(UUID userId) {
        User userToDeactivate = usersRepository.findById(userId)
                .orElseThrow(()-> new UserNotFoundException(""));

        userToDeactivate.setActive(false);
        return usersRepository.save(userToDeactivate);
    }

    public String loginUser(UUID userId, String password) {
        if(!usersRepository.existsById(userId)) {
            throw new LoginFailedException("");
        }

        User user = usersRepository.findById(userId)
                .orElseThrow(()-> new LoginFailedException(""));

        if(!PasswordUtils.checkPassword(password, user.getPassword())) {
            throw new LoginFailedException("");
        }

        return "";
    }
}

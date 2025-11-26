package org.evently.users.services;

import org.evently.users.Utils.JwtUtils;
import org.evently.users.Utils.PasswordUtils;
import org.evently.users.exceptions.*;
import org.evently.users.models.User;
import org.evently.users.repositories.UsersRepository;
import org.modelmapper.ModelMapper;
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

    @Transactional
    public User createUser(User user) {
        if (usersRepository.existsByUsername(user.getUsername())) {
            throw new UserAlreadyExistsException("User with username " + user.getUsername() + " already exists");
        }

        user.setPassword(PasswordUtils.hashPassword(user.getPassword()));

        return usersRepository.save(user);
    }

    @Transactional
    public User updateUser(UUID id,User user) {
        if (!id.equals(user.getId())){
            throw new InvalidUserUpdateException("Parameter id and body id do not correspond");
        }

        User existingUser = usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        existingUser.setUsername(user.getUsername());
        existingUser.setPassword(PasswordUtils.hashPassword(user.getPassword()));
        existingUser.setEmail(user.getEmail());
        existingUser.setBirthDate(user.getBirthDate());
        existingUser.setNif(user.getNif());
        existingUser.setPhoneNumber(user.getPhoneNumber());

        return usersRepository.save(existingUser);
    }

    public User getUser(UUID userId) {
        return usersRepository
                .findById(userId)
                .orElseThrow(()-> new UserNotFoundException(""));
    }

    @Transactional
    public User deactivateUser(UUID userId) {
        User userToDeactivate = usersRepository.findById(userId)
                .orElseThrow(()-> new UserNotFoundException(""));

        if(!userToDeactivate.isActive()){
            throw new UserAlreadyDeactivatedException("");
        }

        userToDeactivate.setActive(false);
        return usersRepository.save(userToDeactivate);
    }

    public String loginUser(String username, String password) {
        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new LoginFailedException("User not found"));

        if (!PasswordUtils.checkPassword(password, user.getPassword())) {
            throw new LoginFailedException("Invalid credentials");
        }

        // gera JWT e devolve
        return jwtUtils.generateToken(user.getId(), user.getUsername());
    }

    public Page<User> getUsersPage(Integer pageNumber, Integer pageSize) {
        if(pageSize>50){
            pageSize = 50;
        }
        PageRequest pageable =  PageRequest.of(pageNumber, pageSize);
        return usersRepository.findAll(pageable);
    }
}

package org.evently.users.services;

import org.evently.users.dtos.UserCreateDTO;
import org.evently.users.dtos.UserDTO;
import org.evently.users.dtos.UserUpdateDTO;
import org.evently.users.models.User;
import org.evently.users.repositories.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    public UserDTO createUser(UserCreateDTO userCreateDTO) {

        return new UserDTO();
    }

    @Transactional
    public UserDTO updateUser(UserUpdateDTO userUpdateDTO) {
        return new UserDTO();
    }

    public UserDTO getUser(UUID userId) {
        return usersRepository.findBy


    }

    public List<UserDTO> getUsersPage() {
        return new ArrayList<UserDTO>();
    }

    @Transactional
    public UserDTO deactivateUser(UUID userId) {
        return new UserDTO();
    }

    public String loginUser(User user) {
        return "";
    }
}

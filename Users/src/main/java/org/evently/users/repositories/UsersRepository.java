package org.evently.users.repositories;

import org.evently.users.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsersRepository extends JpaRepository<User, UUID> {
    
    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);
}

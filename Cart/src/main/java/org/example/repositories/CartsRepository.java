package org.example.repositories;

import org.example.models.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartsRepository extends JpaRepository<Cart, UUID> {

}

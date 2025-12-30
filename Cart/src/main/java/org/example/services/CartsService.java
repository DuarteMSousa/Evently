package org.example.services;

import jakarta.transaction.Transactional;
import org.example.exceptions.CartAlreadyExistsException;
import org.example.exceptions.CartNotFoundException;
import org.example.models.Cart;
import org.example.models.CartItem;
import org.example.repositories.CartsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Service
public class CartsService {

    private Logger logger = LoggerFactory.getLogger(CartsService.class);

    private static final Marker CART_GET = MarkerFactory.getMarker("CART_GET");
    private static final Marker CART_CLEAR = MarkerFactory.getMarker("CART_CLEAR");
    private static final Marker CART_CREATE = MarkerFactory.getMarker("CART_CREATE");


    @Autowired
    private CartsRepository cartsRepository;

    @Transactional
    public Cart getCart(UUID userId) {
        logger.info(CART_GET, "Method getCart entered");
        Cart cart = cartsRepository.findById(userId).orElse(null);
        if (cart == null) {
            logger.info(CART_GET, "Cart not found, creating a new one");
            cart = createCart(userId);
        }

        return cart;
    }

    @Transactional
    public Cart createCart(UUID userId) {
        logger.info(CART_CREATE, "CreateCart Method entered");

        if (cartsRepository.existsById(userId)) {
            logger.error(CART_CREATE, "Cart for user {} already exists",userId);
            throw new CartAlreadyExistsException("Cart already exists");
        }

        Cart newCart = new Cart();

        newCart.setUserId(userId);

        newCart = cartsRepository.save(newCart);

        return newCart;
    }


    @Transactional
    public Cart clearCart(UUID userId) {
        logger.info(CART_CLEAR, "clearCart method entered");

        Cart cartToClear = cartsRepository
                .findById(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        cartToClear.setItems(new ArrayList<CartItem>());

        return cartsRepository.save(cartToClear);
    }

}

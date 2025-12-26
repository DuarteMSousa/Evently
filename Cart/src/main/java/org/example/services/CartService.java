package org.example.services;

import jakarta.transaction.Transactional;
import org.example.exceptions.CartItemAlreadyExistsException;
import org.example.exceptions.CartItemNotFoundException;
import org.example.exceptions.CartNotFoundException;
import org.example.models.Cart;
import org.example.models.CartItem;
import org.example.repositories.CartsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {

    @Autowired
    private CartsRepository cartsRepository;

    @Transactional
    public Cart getCart(UUID userId) {
        Cart cart = cartsRepository.findById(userId).orElse(null);
        if (cart == null) {
            cart = createCart(userId);
        }

        return cart;
    }

    @Transactional
    public Cart createCart(UUID userId) {
        Cart newCart = new Cart();

        newCart.setUserId(userId);

        cartsRepository.save(newCart);

        return cartsRepository.save(newCart);
    }


    @Transactional
    public Cart clearCart(UUID userId) {
        Cart cartToClear = cartsRepository
                .findById(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        cartToClear.setItems(new ArrayList<CartItem>());

        return cartsRepository.save(cartToClear);
    }


    @Transactional
    public Cart addItemToCart(UUID userId, UUID productId, Integer quantity) {
        Cart cart = cartsRepository
                .findById(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        Optional<CartItem> item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        //verificar existencia do product

        if (item.isPresent()) {
            throw new CartItemAlreadyExistsException("Cart Item already exists");
        } else {
            CartItem itemToAdd = new CartItem();
            itemToAdd.setProductId(productId);
            itemToAdd.setQuantity(quantity);
            cart.getItems().add(itemToAdd);
        }

        return cartsRepository.save(cart);
    }

    @Transactional
    public Cart updateCartItem(UUID userId, UUID productId, Integer quantity) {
        Cart cart = cartsRepository
                .findById(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        Optional<CartItem> item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (item.isPresent()) {
            CartItem i = item.get();
            i.setQuantity(quantity);
        } else {
            throw new CartItemNotFoundException("Cart Item not found");
        }

        return cartsRepository.save(cart);
    }


    @Transactional
    public Cart removeItemFromCart(UUID userId, UUID productId) {
        Cart cart = cartsRepository
                .findById(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));

        CartItem itemToRemove = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException("Item not found in cart"));

        cart.getItems().remove(itemToRemove);

        return cartsRepository.save(cart);
    }

}

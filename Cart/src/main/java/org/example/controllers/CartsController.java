package org.example.controllers;

import org.example.dtos.cart.CartDTO;
import org.example.dtos.cartItems.CartItemDTO;
import org.example.exceptions.CartItemAlreadyExistsException;
import org.example.exceptions.CartItemNotFoundException;
import org.example.exceptions.CartNotFoundException;
import org.example.exceptions.EmptyCartException;
import org.example.services.CartsService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/carts")
public class CartsController {

    @Autowired
    private CartsService cartService;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(CartsController.class);

    private static final Marker CART_GET = MarkerFactory.getMarker("CART_GET");
    private static final Marker CART_CLEAR = MarkerFactory.getMarker("CART_CLEAR");
    private static final Marker CART_CHECKOUT = MarkerFactory.getMarker("CART_CHECKOUT");

    @GetMapping("/get-cart/{userId}")
    public ResponseEntity<?> getCart(@PathVariable("userId") UUID userId) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(CART_GET, "Method getCart entered");
        CartDTO cart;

        try {
            cart = modelMapper.map(cartService.getCart(userId), CartDTO.class);
        } catch (Exception e) {
            logger.error(CART_GET, "Exception caught while getting cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(CART_GET, "200 OK returned, cart found");
        return ResponseEntity.status(HttpStatus.OK).body(cart);
    }

    @PutMapping("/clear-cart/{userId}")
    public ResponseEntity<?> clearCart(@PathVariable("userId") UUID userId) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 404 NOT_FOUND - Cart not found.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(CART_CLEAR, "Method clearCart entered");
        CartDTO cart;

        try {
            cart = modelMapper.map(cartService.clearCart(userId), CartDTO.class);
        } catch (CartNotFoundException e) {
            logger.error(CART_CLEAR, "CartNotFoundException caught while clearing cart");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            logger.error(CART_CLEAR, "Exception caught while clearing cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(CART_CLEAR, "200 OK returned, cart clear");
        return ResponseEntity.status(HttpStatus.OK).body(cart);
    }

    @PostMapping("/checkout-cart/{userId}")
    public ResponseEntity<?> checkoutCart(@PathVariable("userId") UUID userId) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Empty cart.
         * 404 NOT_FOUND - Cart not found.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(CART_CHECKOUT, "Method checkoutCart entered");
        CartDTO cart;

        try {
            cart = modelMapper.map(cartService.checkoutCart(userId), CartDTO.class);
        } catch (CartNotFoundException e) {
            logger.error(CART_CHECKOUT, "CartNotFoundException caught while checking out cart");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (EmptyCartException e) {
            logger.error(CART_CHECKOUT, "EmptyCartException caught while checking out cart");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(CART_CHECKOUT, "Exception caught while checking out cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(CART_CHECKOUT, "200 OK returned, cart checked out");
        return ResponseEntity.status(HttpStatus.OK).body(cart);
    }

}

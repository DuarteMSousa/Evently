package org.example.controllers;

import org.example.dtos.cart.CartDTO;
import org.example.dtos.cartItems.CartItemDTO;
import org.example.exceptions.CartItemAlreadyExistsException;
import org.example.exceptions.CartItemNotFoundException;
import org.example.exceptions.CartNotFoundException;
import org.example.services.CartItemsService;
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
@RequestMapping("/carts/items")
public class CartItemsController {

    @Autowired
    private CartItemsService cartItemsService;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(CartItemsController.class);

    private Marker addMarker = MarkerFactory.getMarker("AddCartItem");

    private Marker updateMarker = MarkerFactory.getMarker("UpdateCartItem");

    private Marker removeMarker = MarkerFactory.getMarker("RemoveCartItem");
    
    
    @PostMapping("/add-item/{userId}/{itemId}/{quantity}")
    public ResponseEntity<?> addCartItem(@PathVariable("userId") UUID userId, @PathVariable("itemId") UUID itemId, @PathVariable("quantity") int quantity) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid cart item add.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(addMarker, "Method addCartItem entered");
        CartItemDTO cartItemDto;

        try {
            cartItemDto = modelMapper.map(cartItemsService.addItemToCart(userId, itemId, quantity), CartItemDTO.class);
        } catch (CartNotFoundException e) {
            logger.error(addMarker, "CartNotFoundException caught while adding item to cart");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (CartItemAlreadyExistsException e) {
            logger.error(addMarker, "CartItemAlreadyExistsException caught while adding item to cart");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(addMarker, "Exception caught while adding item to cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(addMarker, "200 OK returned, cart item added");
        return ResponseEntity.status(HttpStatus.OK).body(cartItemDto);
    }

    @PutMapping("/update-item/{userId}/{itemId}/{quantity}")
    public ResponseEntity<?> updateCartItem(@PathVariable("userId") UUID userId, @PathVariable("itemId") UUID itemId, @PathVariable("quantity") int quantity) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid cart item update.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(updateMarker, "Method updateCartItem entered");
        CartItemDTO cartItemDto;

        try {
            cartItemDto = modelMapper.map(cartItemsService.updateCartItem(userId, itemId, quantity), CartItemDTO.class);
        } catch (CartNotFoundException e) {
            logger.error(updateMarker, "CartNotFoundException caught while updating cart item");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (CartItemNotFoundException e) {
            logger.error(updateMarker, "CartItemNotFoundException caught while updating cart item");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(updateMarker, "Exception caught while updating cart item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(updateMarker, "200 OK returned, cart item updated");
        return ResponseEntity.status(HttpStatus.OK).body(cartItemDto);
    }

    @DeleteMapping("/remove-item/{userId}/{itemId}")
    public ResponseEntity<?> removeCartItem(@PathVariable("userId") UUID userId, @PathVariable("itemId") UUID itemId) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event cancellation.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(removeMarker, "Method removeCartItem entered");
        CartItemDTO cartItemDto;

        try {
            cartItemsService.removeItemFromCart(userId, itemId);
        } catch (CartNotFoundException e) {
            logger.error(removeMarker, "CartNotFoundException caught while removing item from cart");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (CartItemNotFoundException e) {
            logger.error(removeMarker, "CartItemNotFoundException caught while removing item from cart");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error(removeMarker, "Exception caught while removing item from cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(removeMarker, "200 OK returned, cart item removed");
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}

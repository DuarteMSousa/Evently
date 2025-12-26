package org.example.controllers;

import org.example.dtos.cart.CartDTO;
import org.example.services.CartService;
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
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    private ModelMapper modelMapper = new ModelMapper();

    private Logger logger = LoggerFactory.getLogger(CartController.class);

    private Marker marker = MarkerFactory.getMarker("CartController");


    @GetMapping("/get-cart/{userId}")
    public ResponseEntity<?> getCart(@PathVariable("userId") UUID userId) {
        /* HttpStatus(produces)
         * 200 OK - Request processed as expected.
         * 400 BAD_REQUEST - Invalid event cancellation.
         * 500 INTERNAL_SERVER_ERROR - Internal server error.
         */
        logger.info(marker, "Method getCart entered");
        CartDTO cart;

        try {
            cart = modelMapper.map(cartService.getCart(userId), CartDTO.class);
        } catch (Exception e) {
            logger.error(marker, "Exception caught while getting cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        logger.info(marker, "200 OK returned, cart found");
        return ResponseEntity.status(HttpStatus.OK).body(cart);
    }

//    @PostMapping("/add-item/{userId}/{itemId}/{quantity}")
//    public ResponseEntity<?> addCartItem(@PathVariable("userId") UUID userId, @PathVariable("itemId") UUID itemId, @PathVariable("quantity") int quantity) {
//        /* HttpStatus(produces)
//         * 200 OK - Request processed as expected.
//         * 400 BAD_REQUEST - Invalid event cancellation.
//         * 500 INTERNAL_SERVER_ERROR - Internal server error.
//         */
//        logger.info(marker, "Method addCartItem entered");
//        CartItemDTO cart;
//
//        try {
//            cart = modelMapper.map(cartService.getCart(userId), CartDTO.class);
//        } catch (Exception e) {
//            logger.error(marker, "Exception caught while getting cart: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
//        }
//
//        logger.info(marker, "200 OK returned, cart found");
//        return ResponseEntity.status(HttpStatus.OK).body(cart);
//    }

}

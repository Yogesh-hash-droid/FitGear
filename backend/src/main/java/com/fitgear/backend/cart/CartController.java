package com.fitgear.backend.cart;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** Cart with server-computed subtotal and item count. */
    @GetMapping
    public CartResponse getCart(Authentication authentication) {
        return cartService.getCart(authentication.getName());
    }

    @GetMapping("/items")
    public List<CartItemResponse> getItems(Authentication authentication) {
        return cartService.getItems(authentication.getName());
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody CartItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItem(authentication.getName(), request));
    }

    @PutMapping("/items/{itemId}")
    public CartItemResponse updateItem(
            Authentication authentication,
            @PathVariable Long itemId,
            @Valid @RequestBody CartQuantityRequest request
    ) {
        return cartService.updateItem(authentication.getName(), itemId, request.quantity());
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(Authentication authentication, @PathVariable Long itemId) {
        cartService.removeItem(authentication.getName(), itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        cartService.clearCart(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}

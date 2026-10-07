package com.fitgear.backend.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cart/items")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public List<CartItemResponse> getCart(Authentication authentication) {
        return cartService.getCart(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<CartItemResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody CartItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItem(authentication.getName(), request));
    }

    @PutMapping("/{itemId}")
    public CartItemResponse updateItem(
            Authentication authentication,
            @PathVariable Long itemId,
            @RequestParam @Min(1) Integer quantity
    ) {
        return cartService.updateItem(authentication.getName(), itemId, quantity);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeItem(Authentication authentication, @PathVariable Long itemId) {
        cartService.removeItem(authentication.getName(), itemId);
        return ResponseEntity.noContent().build();
    }
}

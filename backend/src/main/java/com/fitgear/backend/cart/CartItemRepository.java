package com.fitgear.backend.cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserEmailIgnoreCase(String email);

    Optional<CartItem> findByIdAndUserEmailIgnoreCase(Long id, String email);

    Optional<CartItem> findByUserEmailIgnoreCaseAndProductId(String email, Long productId);
}

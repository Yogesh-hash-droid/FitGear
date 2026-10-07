package com.fitgear.backend.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    Optional<Order> findByIdAndUserEmailIgnoreCase(Long id, String email);
}

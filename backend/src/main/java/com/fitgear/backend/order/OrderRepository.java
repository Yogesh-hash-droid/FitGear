package com.fitgear.backend.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * All accessor queries fetch user + items + payment in one shot so the
     * service can build DTOs inside the transaction (no lazy-loading surprises,
     * no N+1 queries).
     */
    @Override
    @EntityGraph(attributePaths = {"user", "items", "payment"})
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = {"user", "items", "payment"})
    List<Order> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"user", "items", "payment"})
    Optional<Order> findByIdAndUserEmailIgnoreCase(Long id, String email);

    @EntityGraph(attributePaths = {"user", "items", "payment"})
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "items", "payment"})
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status <> :excluded")
    BigDecimal sumTotalExcluding(@Param("excluded") OrderStatus excluded);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatusGrouped();
}

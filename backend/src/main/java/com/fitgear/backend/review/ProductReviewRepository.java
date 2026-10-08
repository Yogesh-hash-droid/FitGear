package com.fitgear.backend.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    Page<ProductReview> findByProductId(Long productId, Pageable pageable);

    Optional<ProductReview> findByProductIdAndUserId(Long productId, Long userId);

    long countByProductId(Long productId);

    /** Null when the product has no reviews yet. */
    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.id = :productId")
    Double findAverageRating(@Param("productId") Long productId);

    /** Histogram rows, only for star values that actually occur. */
    @Query("""
            SELECT new com.fitgear.backend.review.RatingCount(r.rating, COUNT(r))
            FROM ProductReview r
            WHERE r.product.id = :productId
            GROUP BY r.rating
            """)
    List<RatingCount> countRatingsByProduct(@Param("productId") Long productId);
}

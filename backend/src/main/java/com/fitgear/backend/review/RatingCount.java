package com.fitgear.backend.review;

/**
 * One row of the per-star histogram for a product, e.g. (5, 12) means
 * twelve 5-star reviews. Produced by a JPQL constructor expression, so the
 * argument order must match the SELECT clause.
 */
public record RatingCount(Integer rating, Long count) {
}

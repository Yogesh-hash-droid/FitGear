package com.fitgear.backend.review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String userName,
        Integer rating,
        String title,
        String body,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReviewResponse from(ProductReview review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getUser().getName(),
                review.getRating(),
                review.getTitle(),
                review.getBody(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}

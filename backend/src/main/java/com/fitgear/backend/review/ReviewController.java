package com.fitgear.backend.review;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Reads are public (they hang off the product resource), writes require a
 * signed-in customer. The admin role is allowed through as well so a staff
 * account can moderate or correct a review.
 */
@RestController
public class ReviewController {

    /** Sort keys clients are allowed to use - anything else falls back to newest first. */
    private static final Set<String> SORTABLE = Set.of("createdAt", "rating", "id");

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/products/{productId}/reviews")
    public Page<ReviewResponse> getReviews(@PathVariable Long productId, Pageable pageable) {
        return reviewService.getReviews(productId, safeSort(pageable));
    }

    @GetMapping("/api/products/{productId}/reviews/summary")
    public ReviewSummaryResponse getSummary(@PathVariable Long productId) {
        return reviewService.getSummary(productId);
    }

    /** The signed-in customer's own review, or 204 when they have not written one. */
    @GetMapping("/api/reviews/mine")
    public ResponseEntity<ReviewResponse> getMyReview(
            @RequestParam Long productId,
            Authentication authentication
    ) {
        ReviewResponse review = reviewService.getMyReview(productId, authentication.getName());
        return review == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(review);
    }

    @PostMapping("/api/products/{productId}/reviews")
    public ResponseEntity<ReviewResponse> saveReview(
            Authentication authentication,
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request
    ) {
        ReviewResponse saved = reviewService.saveReview(authentication.getName(), productId, request);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(Authentication authentication, @PathVariable Long reviewId) {
        reviewService.deleteReview(authentication.getName(), reviewId, isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private Pageable safeSort(Pageable pageable) {
        List<Sort.Order> allowed = pageable.getSort().stream()
                .filter(order -> SORTABLE.contains(order.getProperty()))
                .toList();
        Sort sort = allowed.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(allowed);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}

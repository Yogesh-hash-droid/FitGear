package com.fitgear.backend.review;

import com.fitgear.backend.auth.AppUser;
import com.fitgear.backend.auth.UserNotFoundException;
import com.fitgear.backend.auth.UserRepository;
import com.fitgear.backend.product.Product;
import com.fitgear.backend.product.ProductNotFoundException;
import com.fitgear.backend.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product reviews and their aggregate rating.
 *
 * <p>A customer has at most one review per product: creating a review that
 * already exists updates it in place. Ownership is enforced here rather than
 * in the controller so the rule holds for every caller.</p>
 */
@Service
public class ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewService(
            ProductReviewRepository reviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    /** Paged reviews for a product, newest first unless the caller sorts. */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(Long productId, Pageable pageable) {
        requireProduct(productId);
        return reviewRepository.findByProductId(productId, pageable).map(ReviewResponse::from);
    }

    /** Average rating, total and per-star histogram. */
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummary(Long productId) {
        requireProduct(productId);
        Double average = reviewRepository.findAverageRating(productId);
        long total = reviewRepository.countByProductId(productId);
        return ReviewSummaryResponse.of(average == null ? 0d : average, total,
                reviewRepository.countRatingsByProduct(productId));
    }

    /** The caller's own review, or {@code null} when they have not written one. */
    @Transactional(readOnly = true)
    public ReviewResponse getMyReview(Long productId, String email) {
        requireProduct(productId);
        AppUser user = requireUser(email);
        return reviewRepository.findByProductIdAndUserId(productId, user.getId())
                .map(ReviewResponse::from)
                .orElse(null);
    }

    /** Creates the caller's review, or replaces it if they already have one. */
    @Transactional
    public ReviewResponse saveReview(String email, Long productId, ReviewRequest request) {
        Product product = requireProduct(productId);
        AppUser user = requireUser(email);

        ProductReview review = reviewRepository
                .findByProductIdAndUserId(productId, user.getId())
                .orElseGet(() -> new ProductReview(product, user));

        review.setRating(request.getRating());
        review.setTitle(normalize(request.getTitle()));
        review.setBody(normalize(request.getBody()));
        return ReviewResponse.from(reviewRepository.save(review));
    }

    /**
     * Removes a review. Admins may remove any review; everyone else only
     * their own.
     */
    @Transactional
    public void deleteReview(String email, Long reviewId, boolean admin) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        if (!admin) {
            AppUser user = requireUser(email);
            if (!review.getUser().getId().equals(user.getId())) {
                throw new ReviewAccessException();
            }
        }
        reviewRepository.delete(review);
    }

    private Product requireProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private AppUser requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
    }

    /** Blank optional fields are stored as null rather than empty strings. */
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

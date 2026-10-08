package com.fitgear.backend.review;

import com.fitgear.backend.auth.AppUser;
import com.fitgear.backend.auth.UserNotFoundException;
import com.fitgear.backend.auth.UserRepository;
import com.fitgear.backend.auth.UserRole;
import com.fitgear.backend.product.Product;
import com.fitgear.backend.product.ProductNotFoundException;
import com.fitgear.backend.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class ReviewServiceTest {

    private ProductReviewRepository reviewRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;
    private ReviewService service;

    private Product product;
    private AppUser user;

    @BeforeEach
    void setUp() {
        reviewRepository = Mockito.mock(ProductReviewRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        service = new ReviewService(reviewRepository, productRepository, userRepository);

        product = new Product("Squat Rack Pro", "Heavy gauge", new BigDecimal("18999"), 6);
        product.setId(1L);
        user = new AppUser("Arun", "arun@example.com", "hashed", UserRole.CUSTOMER);
        user.setId(7L);

        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(userRepository.findByEmailIgnoreCase("arun@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void shouldCreateFirstReviewForProduct() {
        Mockito.when(reviewRepository.findByProductIdAndUserId(1L, 7L)).thenReturn(Optional.empty());
        Mockito.when(reviewRepository.save(any(ProductReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = service.saveReview(
                "arun@example.com", 1L, new ReviewRequest(5, "Rock solid", "Holds heavy loads."));

        assertEquals(5, response.rating());
        assertEquals(1L, response.productId());
        assertEquals(7L, response.userId());
        assertEquals("Arun", response.userName());
        assertEquals("Rock solid", response.title());
        Mockito.verify(reviewRepository).save(any(ProductReview.class));
    }

    @Test
    void shouldUpdateExistingReviewInsteadOfCreatingSecond() {
        ProductReview existing = new ProductReview(product, user);
        existing.setId(9L);
        existing.setRating(3);
        existing.setTitle("Meh");
        Mockito.when(reviewRepository.findByProductIdAndUserId(1L, 7L)).thenReturn(Optional.of(existing));
        Mockito.when(reviewRepository.save(any(ProductReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = service.saveReview(
                "arun@example.com", 1L, new ReviewRequest(5, "Revised", "Grew on me."));

        // Same row, updated in place - the unique (user, product) key holds.
        assertEquals(9L, response.id());
        assertEquals(5, response.rating());
        assertEquals("Revised", response.title());
        Mockito.verify(reviewRepository).save(any(ProductReview.class));
    }

    @Test
    void shouldStoreBlankOptionalFieldsAsNull() {
        Mockito.when(reviewRepository.findByProductIdAndUserId(1L, 7L)).thenReturn(Optional.empty());
        Mockito.when(reviewRepository.save(any(ProductReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = service.saveReview(
                "arun@example.com", 1L, new ReviewRequest(4, "   ", null));

        assertNull(response.title());
        assertNull(response.body());
        assertEquals(4, response.rating());
    }

    @Test
    void shouldRejectReviewForUnknownProduct() {
        assertThrows(ProductNotFoundException.class,
                () -> service.saveReview("arun@example.com", 99L, new ReviewRequest(5, null, null)));
        assertThrows(ProductNotFoundException.class,
                () -> service.getSummary(99L));
    }

    @Test
    void shouldRejectReviewFromUnknownUser() {
        Mockito.when(userRepository.findByEmailIgnoreCase("ghost@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.saveReview("ghost@example.com", 1L, new ReviewRequest(5, null, null)));
    }

    @Test
    void shouldReturnNullWhenCallerHasNotReviewed() {
        Mockito.when(reviewRepository.findByProductIdAndUserId(1L, 7L)).thenReturn(Optional.empty());

        assertNull(service.getMyReview(1L, "arun@example.com"));
    }

    @Test
    void shouldLetOwnerDeleteTheirReview() {
        ProductReview existing = new ProductReview(product, user);
        existing.setId(3L);
        Mockito.when(reviewRepository.findById(3L)).thenReturn(Optional.of(existing));

        service.deleteReview("arun@example.com", 3L, false);

        Mockito.verify(reviewRepository).delete(existing);
    }

    @Test
    void shouldBlockDeletingSomeoneElsesReview() {
        AppUser other = new AppUser("Meera", "meera@example.com", "hashed", UserRole.CUSTOMER);
        other.setId(8L);
        ProductReview existing = new ProductReview(product, other);
        existing.setId(3L);
        Mockito.when(reviewRepository.findById(3L)).thenReturn(Optional.of(existing));

        assertThrows(ReviewAccessException.class,
                () -> service.deleteReview("arun@example.com", 3L, false));
        Mockito.verify(reviewRepository, Mockito.never()).delete(any(ProductReview.class));
    }

    @Test
    void shouldLetAdminDeleteAnyReview() {
        AppUser other = new AppUser("Meera", "meera@example.com", "hashed", UserRole.CUSTOMER);
        other.setId(8L);
        ProductReview existing = new ProductReview(product, other);
        existing.setId(3L);
        Mockito.when(reviewRepository.findById(3L)).thenReturn(Optional.of(existing));

        service.deleteReview("admin@fitgear.local", 3L, true);

        Mockito.verify(reviewRepository).delete(existing);
    }

    @Test
    void shouldReportUnknownReviewAsNotFound() {
        Mockito.when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ReviewNotFoundException.class,
                () -> service.deleteReview("arun@example.com", 404L, false));
    }

    @Test
    void shouldTreatMissingAverageAsZeroStars() {
        Mockito.when(reviewRepository.findAverageRating(1L)).thenReturn(null);
        Mockito.when(reviewRepository.countByProductId(1L)).thenReturn(0L);
        Mockito.when(reviewRepository.countRatingsByProduct(1L)).thenReturn(java.util.List.of());

        ReviewSummaryResponse summary = service.getSummary(1L);

        assertEquals(0.0, summary.averageRating());
        assertEquals(0, summary.totalReviews());
    }

    @Test
    void shouldExposeRepositoryAverageAsSummary() {
        Mockito.when(reviewRepository.findAverageRating(1L)).thenReturn(4.25d);
        Mockito.when(reviewRepository.countByProductId(1L)).thenReturn(4L);
        Mockito.when(reviewRepository.countRatingsByProduct(1L))
                .thenReturn(java.util.List.of(new RatingCount(5, 2L), new RatingCount(4, 2L)));

        ReviewSummaryResponse summary = service.getSummary(1L);

        assertEquals(4.3, summary.averageRating());
        assertEquals(4, summary.totalReviews());
        assertEquals(2L, summary.distribution().get(5));
        assertEquals(0L, summary.distribution().get(2));
    }
}

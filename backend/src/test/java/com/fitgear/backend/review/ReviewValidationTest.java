package com.fitgear.backend.review;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void shouldRejectMissingRating() {
        Set<String> fields = fieldsOf(new ReviewRequest(null, "Solid", "Good grip."));

        assertEquals(Set.of("rating"), fields);
    }

    @Test
    void shouldRejectRatingOutsideOneToFive() {
        Set<String> tooLow = fieldsOf(new ReviewRequest(0, null, null));
        Set<String> tooHigh = fieldsOf(new ReviewRequest(6, null, null));

        assertEquals(Set.of("rating"), tooLow);
        assertEquals(Set.of("rating"), tooHigh);
    }

    @Test
    void shouldRejectOversizedTitleAndBody() {
        Set<String> fields = fieldsOf(new ReviewRequest(
                5,
                "x".repeat(121),
                "y".repeat(2001)
        ));

        assertEquals(Set.of("title", "body"), fields);
    }

    @Test
    void shouldAcceptValidReviewRequest() {
        assertTrue(validator.validate(new ReviewRequest(4, "Solid rack", "Stable under load.")).isEmpty());
    }

    @Test
    void shouldAcceptMinimalReviewRequest() {
        assertTrue(validator.validate(new ReviewRequest(1, null, null)).isEmpty());
    }

    @Test
    void shouldRoundAverageAndReportEveryStarBucket() {
        ReviewSummaryResponse summary = ReviewSummaryResponse.of(
                4.25, 3, java.util.List.of(new RatingCount(5, 2L), new RatingCount(4, 1L)));

        assertEquals(4.3, summary.averageRating());
        assertEquals(3, summary.totalReviews());
        // Every bucket is present, 5 first, with zeros for the unused stars.
        assertEquals(java.util.List.of(5, 4, 3, 2, 1),
                new java.util.ArrayList<>(summary.distribution().keySet()));
        assertEquals(2L, summary.distribution().get(5));
        assertEquals(0L, summary.distribution().get(1));
    }

    @Test
    void shouldReportZeroSummaryForProductWithoutReviews() {
        ReviewSummaryResponse summary = ReviewSummaryResponse.of(0d, 0, java.util.List.of());

        assertEquals(0.0, summary.averageRating());
        assertEquals(0, summary.totalReviews());
        assertEquals(0L, summary.distribution().get(3));
    }

    private Set<String> fieldsOf(ReviewRequest request) {
        return validator.validate(request).stream()
                .map(error -> error.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}

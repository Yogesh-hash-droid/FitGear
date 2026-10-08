package com.fitgear.backend.review;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate rating for one product: rounded average, total count and a
 * 5 -> 1 star histogram that always reports every star value so the UI can
 * render five bars without filling in the gaps itself.
 */
public record ReviewSummaryResponse(
        double averageRating,
        long totalReviews,
        Map<Integer, Long> distribution
) {

    public static ReviewSummaryResponse of(double averageRating, long totalReviews, List<RatingCount> counts) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            distribution.put(stars, 0L);
        }
        if (counts != null) {
            for (RatingCount count : counts) {
                if (count != null && count.rating() != null && distribution.containsKey(count.rating())) {
                    distribution.put(count.rating(), count.count() == null ? 0L : count.count());
                }
            }
        }
        double rounded = BigDecimal.valueOf(averageRating)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
        return new ReviewSummaryResponse(rounded, totalReviews, Collections.unmodifiableMap(distribution));
    }
}

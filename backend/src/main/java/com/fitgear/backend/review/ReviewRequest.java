package com.fitgear.backend.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or updating the caller's own review of a product.
 * Posting a second review for the same product replaces the first one.
 */
public class ReviewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 120, message = "Title must be 120 characters or fewer")
    private String title;

    @Size(max = 2000, message = "Review must be 2000 characters or fewer")
    private String body;

    public ReviewRequest() {
    }

    public ReviewRequest(Integer rating, String title, String body) {
        this.rating = rating;
        this.title = title;
        this.body = body;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}

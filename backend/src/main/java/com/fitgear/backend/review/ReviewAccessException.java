package com.fitgear.backend.review;

/** Thrown when a customer tries to remove a review that is not theirs. */
public class ReviewAccessException extends RuntimeException {

    public ReviewAccessException() {
        super("You can only remove your own review");
    }
}

package com.fitgear.backend.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        int status,
        String message,
        Instant timestamp,
        Map<String, String> errors
) {
}

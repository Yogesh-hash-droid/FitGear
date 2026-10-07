package com.fitgear.backend.exception;

import com.fitgear.backend.product.ProductNotFoundException;
import com.fitgear.backend.auth.EmailAlreadyRegisteredException;
import com.fitgear.backend.auth.InvalidCredentialsException;
import com.fitgear.backend.cart.CartItemNotFoundException;
import com.fitgear.backend.cart.InsufficientStockException;
import com.fitgear.backend.order.OrderCancellationException;
import com.fitgear.backend.order.EmptyCartException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler({CartItemNotFoundException.class, InsufficientStockException.class})
    public ResponseEntity<ApiError> handleCartError(RuntimeException exception) {
        HttpStatus status = exception instanceof CartItemNotFoundException
                ? HttpStatus.NOT_FOUND
                : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(new ApiError(
                status.value(), exception.getMessage(), Instant.now(), Map.of()
        ));
    }

        @ExceptionHandler(OrderCancellationException.class)
        public ResponseEntity<ApiError> handleOrderCancellation(OrderCancellationException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                                HttpStatus.CONFLICT.value(), exception.getMessage(), Instant.now(), Map.of()
                ));
        }

        @ExceptionHandler(EmptyCartException.class)
        public ResponseEntity<ApiError> handleEmptyCart(EmptyCartException exception) {
                return ResponseEntity.badRequest().body(new ApiError(400, exception.getMessage(), Instant.now(), Map.of()));
        }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Unexpected server error",
                Instant.now(),
                Map.of()
        ));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(EmailAlreadyRegisteredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                HttpStatus.CONFLICT.value(),
                exception.getMessage(),
                Instant.now(),
                Map.of()
        ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                exception.getMessage(),
                Instant.now(),
                Map.of()
        ));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> handleProductNotFound(ProductNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage(),
                Instant.now(),
                Map.of()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Request validation failed",
                Instant.now(),
                errors
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage(),
                Instant.now(),
                Map.of()
        ));
    }
}

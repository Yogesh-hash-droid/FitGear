package com.fitgear.backend.exception;

import com.fitgear.backend.auth.EmailAlreadyRegisteredException;
import com.fitgear.backend.auth.InvalidCredentialsException;
import com.fitgear.backend.auth.UserNotFoundException;
import com.fitgear.backend.cart.CartItemNotFoundException;
import com.fitgear.backend.cart.InsufficientStockException;
import com.fitgear.backend.order.EmptyCartException;
import com.fitgear.backend.order.InvalidOrderTransitionException;
import com.fitgear.backend.order.OrderCancellationException;
import com.fitgear.backend.order.OrderNotFoundException;
import com.fitgear.backend.payment.PaymentException;
import com.fitgear.backend.payment.PaymentNotConfiguredException;
import com.fitgear.backend.payment.PaymentVerificationException;
import com.fitgear.backend.product.ProductNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps every exception to a consistent JSON error body:
 * { "status": 404, "message": "...", "timestamp": "...", "errors": { field: msg } }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiError> body(HttpStatus status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), message, Instant.now(), errors));
    }

    private ResponseEntity<ApiError> body(HttpStatus status, String message) {
        return body(status, message, Map.of());
    }

    // ---- 400 -------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        return body(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<ApiError> handleEmptyCart(EmptyCartException exception) {
        return body(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException exception) {
        return body(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return body(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + exception.getName() + "'");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException exception) {
        return body(HttpStatus.BAD_REQUEST, "Missing required parameter: " + exception.getParameterName());
    }

    @ExceptionHandler(PaymentVerificationException.class)
    public ResponseEntity<ApiError> handlePaymentVerification(PaymentVerificationException exception) {
        return body(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    // ---- 401 -------------------------------------------------------------

    @ExceptionHandler({InvalidCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiError> handleUnauthorized(RuntimeException exception) {
        String message = exception instanceof InvalidCredentialsException
                ? exception.getMessage()
                : "Authentication required";
        return body(HttpStatus.UNAUTHORIZED, message);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException exception) {
        return body(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    // ---- 403 handled by Spring Security (AccessDeniedHandler) ------------

    // ---- 404 -------------------------------------------------------------

    @ExceptionHandler({ProductNotFoundException.class, OrderNotFoundException.class,
            CartItemNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException exception) {
        return body(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    // ---- 409 Conflict ----------------------------------------------------

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(EmailAlreadyRegisteredException exception) {
        return body(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException exception) {
        return body(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({OrderCancellationException.class, InvalidOrderTransitionException.class,
            PaymentException.class})
    public ResponseEntity<ApiError> handleConflict(RuntimeException exception) {
        return body(HttpStatus.CONFLICT, exception.getMessage());
    }

    // ---- 503 Service unavailable -----------------------------------------

    @ExceptionHandler(PaymentNotConfiguredException.class)
    public ResponseEntity<ApiError> handlePaymentNotConfigured(PaymentNotConfiguredException exception) {
        return body(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    // ---- 405 / concurrency ------------------------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "Method " + exception.getMethod() + " is not supported");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException exception) {
        return body(HttpStatus.CONFLICT, "The record was modified by someone else. Please refresh and try again.");
    }

    // ---- 500 last resort --------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception", exception);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }
}

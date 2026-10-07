package com.fitgear.backend.product;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductValidationTest {

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
    void shouldRejectInvalidProductRequest() {
        ProductRequest request = new ProductRequest("", "Invalid product", BigDecimal.ZERO, -1);

        Set<String> fields = validator.validate(request).stream()
                .map(error -> error.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("name", "price", "stock"), fields);
    }

    @Test
    void shouldAcceptValidProductRequest() {
        ProductRequest request = new ProductRequest(
                "Adjustable Dumbbell",
                "20kg home gym dumbbell",
                new BigDecimal("4999"),
                10
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldMapEntityToResponse() {
        Product product = new Product(
                "Adjustable Dumbbell",
                "20kg home gym dumbbell",
                new BigDecimal("4999"),
                10
        );
        product.setId(1L);

        ProductResponse response = ProductResponse.from(product);

        assertEquals(1L, response.id());
        assertEquals("Adjustable Dumbbell", response.name());
        assertEquals(new BigDecimal("4999"), response.price());
    }
}

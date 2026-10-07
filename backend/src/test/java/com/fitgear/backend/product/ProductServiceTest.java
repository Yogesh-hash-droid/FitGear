package com.fitgear.backend.product;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class ProductServiceTest {

    @Test
    void shouldCreateProduct() {
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        ProductService service = new ProductService(productRepository);

        Product product = new Product("Adjustable Dumbbell", "Home gym dumbbell", new BigDecimal("4999"), 10);
        Mockito.when(productRepository.save(any(Product.class))).thenReturn(product);
        Mockito.when(productRepository.findAll()).thenReturn(List.of(product));

        Product savedProduct = service.createProduct("Adjustable Dumbbell", "Home gym dumbbell", new BigDecimal("4999"), 10, "Strength", null);

        assertEquals("Adjustable Dumbbell", savedProduct.getName());
        assertEquals(1, service.getAllProducts().size());
    }

    @Test
    void shouldSearchProductsWithPagination() {
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        ProductService service = new ProductService(productRepository);
        Product product = new Product("Adjustable Dumbbell", "Home gym dumbbell", new BigDecimal("4999"), 10);
        PageRequest pageRequest = PageRequest.of(0, 10);

        Mockito.when(productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        "dumbbell", "dumbbell", pageRequest))
                .thenReturn(new PageImpl<>(List.of(product), pageRequest, 1));

        assertEquals(1, service.searchProducts("dumbbell", pageRequest).getTotalElements());
        Mockito.verify(productRepository).findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                "dumbbell", "dumbbell", pageRequest);
    }
}

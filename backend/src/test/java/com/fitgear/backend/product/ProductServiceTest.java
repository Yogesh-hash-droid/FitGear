package com.fitgear.backend.product;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

        Product savedProduct = service.createProduct("Adjustable Dumbbell", "Home gym dumbbell", new BigDecimal("4999"), 10, "Strength", null);

        assertEquals("Adjustable Dumbbell", savedProduct.getName());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        Mockito.verify(productRepository).save(captor.capture());
        assertEquals("Strength", captor.getValue().getCategory());
    }

    @Test
    void shouldSearchProductsWithPagination() {
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        ProductService service = new ProductService(productRepository);
        Product product = new Product("Adjustable Dumbbell", "Home gym dumbbell", new BigDecimal("4999"), 10);
        PageRequest pageRequest = PageRequest.of(0, 10);

        Mockito.when(productRepository.search("dumbbell", null, null, null, pageRequest))
                .thenReturn(new PageImpl<>(List.of(product), pageRequest, 1));

        assertEquals(1, service.searchProducts("dumbbell", pageRequest).getTotalElements());
        Mockito.verify(productRepository).search("dumbbell", null, null, null, pageRequest);
    }

    @Test
    void shouldFilterByCategoryAndPriceRange() {
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        ProductService service = new ProductService(productRepository);
        PageRequest pageRequest = PageRequest.of(0, 10);
        Product product = new Product("Kettlebell", "Cast iron", new BigDecimal("2499"), 7);

        Mockito.when(productRepository.search("bell", "Strength", new BigDecimal("1000"), new BigDecimal("5000"), pageRequest))
                .thenReturn(new PageImpl<>(List.of(product), pageRequest, 1));

        assertEquals(1, service.searchProducts("bell", "Strength",
                new BigDecimal("1000"), new BigDecimal("5000"), pageRequest).getTotalElements());
    }

    @Test
    void shouldTreatBlankFiltersAsNull() {
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        ProductService service = new ProductService(productRepository);
        PageRequest pageRequest = PageRequest.of(0, 10);

        Mockito.when(productRepository.search(null, null, null, null, pageRequest))
                .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        service.searchProducts("  ", "  ", null, null, pageRequest);

        Mockito.verify(productRepository).search(null, null, null, null, pageRequest);
    }
}

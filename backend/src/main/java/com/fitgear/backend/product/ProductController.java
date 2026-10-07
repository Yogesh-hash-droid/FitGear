package com.fitgear.backend.product;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        Product createdProduct = productService.createProduct(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getStock(),
                request.getCategory(),
                request.getImageUrl()
        );
        return new ResponseEntity<>(ProductResponse.from(createdProduct), HttpStatus.CREATED);
    }

    @GetMapping
        public Page<ProductResponse> getAllProducts(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String category,
            Pageable pageable
        ) {
        return productService.searchProducts(search, category, pageable)
            .map(ProductResponse::from);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return ProductResponse.from(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        Product updatedProduct = productService.updateProduct(
                id,
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getStock(),
                request.getCategory(),
                request.getImageUrl()
        );
        return ProductResponse.from(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}

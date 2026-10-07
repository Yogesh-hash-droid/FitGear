package com.fitgear.backend.product;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    /** Sort keys clients are allowed to use - anything else falls back to name. */
    private static final Set<String> SORTABLE = Set.of("name", "price", "stock", "category", "createdAt", "id");

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
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable
    ) {
        return productService.searchProducts(search, category, minPrice, maxPrice, safeSort(pageable))
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

    private Pageable safeSort(Pageable pageable) {
        List<Sort.Order> allowed = pageable.getSort().stream()
                .filter(order -> SORTABLE.contains(order.getProperty()))
                .toList();
        Sort sort = allowed.isEmpty()
                ? Sort.by(Sort.Direction.ASC, "name")
                : Sort.by(allowed);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}

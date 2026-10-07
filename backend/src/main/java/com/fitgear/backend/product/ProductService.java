package com.fitgear.backend.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product createProduct(String name, String description, BigDecimal price, Integer stock, String category, String imageUrl) {
        String normalizedCategory = category == null || category.isBlank() ? "Strength" : category;
        Product product = new Product(name, description, price, stock, normalizedCategory, imageUrl);
        return productRepository.save(product);
    }

    public Page<Product> searchProducts(String search, String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        return productRepository.search(normalizedSearch, normalizedCategory, minPrice, maxPrice, pageable);
    }

    public Page<Product> searchProducts(String search, Pageable pageable) {
        return searchProducts(search, null, null, null, pageable);
    }

    public Page<Product> searchProducts(String search, String category, Pageable pageable) {
        return searchProducts(search, category, null, null, pageable);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product updateProduct(Long id, String name, String description, BigDecimal price, Integer stock, String category, String imageUrl) {
        Product existingProduct = getProductById(id);
        existingProduct.setName(name);
        existingProduct.setDescription(description);
        existingProduct.setPrice(price);
        existingProduct.setStock(stock);
        existingProduct.setCategory(category == null || category.isBlank() ? "Strength" : category);
        existingProduct.setImageUrl(imageUrl);
        return productRepository.save(existingProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product existingProduct = getProductById(id);
        productRepository.delete(existingProduct);
    }

    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }
}

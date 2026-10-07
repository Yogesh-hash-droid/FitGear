package com.fitgear.backend.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(String name, String description, BigDecimal price, Integer stock, String category, String imageUrl) {
        String normalizedCategory = category == null || category.isBlank() ? "Strength" : category;
        Product product = new Product(name, description, price, stock, normalizedCategory, imageUrl);
        return productRepository.save(product);
    }

    public Product createProduct(String name, String description, BigDecimal price, Integer stock) {
        return createProduct(name, description, price, stock, null, null);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Page<Product> searchProducts(String search, Pageable pageable) {
        return searchProducts(search, null, pageable);
    }

    public Page<Product> searchProducts(String search, String category, Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCategory = category != null && !category.isBlank();
        if (!hasSearch && !hasCategory) {
            return productRepository.findAll(pageable);
        }
        if (!hasSearch) return productRepository.findByCategoryIgnoreCase(category.trim(), pageable);
        if (!hasCategory) {
            return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search.trim(), search.trim(), pageable);
        }
        return productRepository.findByCategoryAndTerm(category.trim(), search.trim(), pageable);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

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

    public void deleteProduct(Long id) {
        Product existingProduct = getProductById(id);
        productRepository.delete(existingProduct);
    }
}

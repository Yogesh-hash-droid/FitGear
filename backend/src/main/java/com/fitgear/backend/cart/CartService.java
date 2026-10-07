package com.fitgear.backend.cart;

import com.fitgear.backend.auth.AppUser;
import com.fitgear.backend.auth.UserRepository;
import com.fitgear.backend.product.Product;
import com.fitgear.backend.product.ProductNotFoundException;
import com.fitgear.backend.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public List<CartItemResponse> getCart(String email) {
        return cartItemRepository.findByUserEmailIgnoreCase(email).stream()
                .map(CartItemResponse::from)
                .toList();
    }

    @Transactional
    public CartItemResponse addItem(String email, CartItemRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));
        AppUser user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        CartItem item = cartItemRepository.findByUserEmailIgnoreCaseAndProductId(email, product.getId())
            .orElseGet(() -> new CartItem(user, product, 0));
        int newQuantity = item.getQuantity() + request.quantity();
        validateStock(product, newQuantity);
        item.setQuantity(newQuantity);
        return CartItemResponse.from(cartItemRepository.save(item));
    }

    @Transactional
    public CartItemResponse updateItem(String email, Long itemId, Integer quantity) {
        CartItem item = cartItemRepository.findByIdAndUserEmailIgnoreCase(itemId, email)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));
        validateStock(item.getProduct(), quantity);
        item.setQuantity(quantity);
        return CartItemResponse.from(cartItemRepository.save(item));
    }

    @Transactional
    public void removeItem(String email, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndUserEmailIgnoreCase(itemId, email)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));
        cartItemRepository.delete(item);
    }

    private void validateStock(Product product, Integer quantity) {
        if (quantity > product.getStock()) {
            throw new InsufficientStockException(product.getName(), quantity, product.getStock());
        }
    }
}

package com.fitgear.backend.cart;

import com.fitgear.backend.auth.AppUser;
import com.fitgear.backend.auth.UserNotFoundException;
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

    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        return CartResponse.of(toResponses(cartItemRepository.findByUserEmailIgnoreCase(email)));
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> getItems(String email) {
        return toResponses(cartItemRepository.findByUserEmailIgnoreCase(email));
    }

    @Transactional
    public CartItemResponse addItem(String email, CartItemRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        CartItem item = cartItemRepository.findByUserEmailIgnoreCaseAndProductId(email, product.getId())
                .orElseGet(() -> new CartItem(user, product, 0));
        int newQuantity = item.getQuantity() + request.quantity();
        validateQuantity(product, newQuantity);
        item.setQuantity(newQuantity);
        return CartItemResponse.from(cartItemRepository.save(item));
    }

    @Transactional
    public CartItemResponse updateItem(String email, Long itemId, Integer quantity) {
        CartItem item = cartItemRepository.findByIdAndUserEmailIgnoreCase(itemId, email)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));
        validateQuantity(item.getProduct(), quantity);
        item.setQuantity(quantity);
        return CartItemResponse.from(cartItemRepository.save(item));
    }

    @Transactional
    public void removeItem(String email, Long itemId) {
        CartItem item = cartItemRepository.findByIdAndUserEmailIgnoreCase(itemId, email)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(String email) {
        cartItemRepository.deleteAll(cartItemRepository.findByUserEmailIgnoreCase(email));
    }

    private void validateQuantity(Product product, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new InsufficientStockException(product.getName(), 0, product.getStock());
        }
        if (quantity > product.getStock()) {
            throw new InsufficientStockException(product.getName(), quantity, product.getStock());
        }
    }

    private List<CartItemResponse> toResponses(List<CartItem> items) {
        return items.stream().map(CartItemResponse::from).toList();
    }
}

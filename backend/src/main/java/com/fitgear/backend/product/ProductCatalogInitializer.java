package com.fitgear.backend.product;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ProductCatalogInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    public ProductCatalogInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        productRepository.saveAll(List.of(
                product("Adjustable Dumbbell Set", "Space-saving weights for progressive strength training.", "4999", 12, "Strength", null),
                product("Cast Iron Kettlebell 16kg", "A durable training staple for swings, carries, and squats.", "2499", 7, "Strength", null),
                product("Core Stability Mat", "Dense, grippy cushioning for floor work and mobility.", "1299", 28, "Recovery", null),
                product("Foam Recovery Roller", "Firm, textured support for warmups and post-session recovery.", "999", 20, "Recovery", null),
                product("Resistance Band Kit", "Five resistance levels for warmups and full-body training.", "899", 42, "Accessories", null),
                product("Training Jump Rope", "Lightweight speed rope for conditioning anywhere.", "699", 32, "Accessories", null),
                product("Indoor Cycling Bike", "A compact cardio setup for steady rides at home.", "24999", 4, "Cardio", null),
                product("Compact Stepper", "Low-impact cardio training with a small footprint.", "7999", 9, "Cardio", null)
        ));
    }

    private Product product(String name, String description, String price, int stock, String category, String imageUrl) {
        return new Product(name, description, new BigDecimal(price), stock, category, imageUrl);
    }
}

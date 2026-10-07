package com.fitgear.backend.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the demo catalog on first boot (empty database only).
 * Controlled by fitgear.seed.enabled so production can turn it off.
 */
@Component
public class ProductCatalogInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogInitializer.class);

    private final ProductRepository productRepository;
    private final boolean seedEnabled;

    public ProductCatalogInitializer(ProductRepository productRepository,
                                     @Value("${fitgear.seed.enabled:true}") boolean seedEnabled) {
        this.productRepository = productRepository;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        if (productRepository.count() > 0) {
            return;
        }

        productRepository.saveAll(List.of(
                product("Adjustable Dumbbell Set", "Space-saving weights for progressive strength training.", "4999", 12, "Strength"),
                product("Cast Iron Kettlebell 16kg", "A durable training staple for swings, carries, and squats.", "2499", 7, "Strength"),
                product("Olympic Barbell 20kg", "Competition-grade bar with knurled grip and bushings.", "7999", 15, "Strength"),
                product("Core Stability Mat", "Dense, grippy cushioning for floor work and mobility.", "1299", 28, "Recovery"),
                product("Foam Recovery Roller", "Firm, textured support for warmups and post-session recovery.", "999", 20, "Recovery"),
                product("Resistance Band Kit", "Five resistance levels for warmups and full-body training.", "899", 42, "Accessories"),
                product("Training Jump Rope", "Lightweight speed rope for conditioning anywhere.", "699", 32, "Accessories"),
                product("Lifting Grip Pads", "Protective grips for deadlifts and heavy pulls.", "599", 50, "Accessories"),
                product("Indoor Cycling Bike", "A compact cardio setup for steady rides at home.", "24999", 4, "Cardio"),
                product("Compact Stepper", "Low-impact cardio training with a small footprint.", "7999", 9, "Cardio"),
                product("Skipping Rope Timer", "Interval timer for HIIT and circuit sessions.", "1499", 3, "Cardio")
        ));
        log.info("Seeded demo product catalog");
    }

    private Product product(String name, String description, String price, int stock, String category) {
        String imageUrl = "/images/products/" + category.toLowerCase() + ".svg";
        return new Product(name, description, new BigDecimal(price), stock, category, imageUrl);
    }
}

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
                // Dumbbells
                product("Adjustable Dumbbell Set", "Space-saving weights for progressive strength training.", "4999", 12, "Dumbbells", unsplash("photo-1679792756270-c90bb6f34868")),
                product("Hex Dumbbell Pair 15 kg", "Rubber-coated hex dumbbells with knurled handles.", "3499", 18, "Dumbbells", unsplash("photo-1756115484694-009466dbaa67")),
                // Barbells
                product("Olympic Barbell 20kg", "Competition-grade bar with knurled grip and bushings.", "7999", 15, "Barbells", unsplash("photo-1590239480892-9059a902df60")),
                product("EZ Curl Bar", "Compact angled bar for arms and isolation work.", "2799", 22, "Barbells", unsplash("photo-1709315847224-6c8a812e886a")),
                // Weight plates
                product("Olympic Weight Plate 10 kg", "Cast-iron plate with a wide grip opening.", "1499", 40, "Weight Plates", unsplash("photo-1733517301178-312a6f694f2d")),
                product("Bumper Plate Set 60 kg", "Dead-bounce rubber bumper plates for lifting platforms.", "12999", 8, "Weight Plates", unsplash("photo-1718254951550-8724745444b9")),
                // Benches
                product("Adjustable Workout Bench", "Seven-position back pad for flat, incline and decline work.", "8999", 10, "Benches", unsplash("photo-1628935291759-bbaf33a66dc6")),
                product("Flat Utility Bench", "Compact flat bench with a stable steel base.", "4999", 14, "Benches", unsplash("photo-1647456763515-6a95d68c192f")),
                // Racks
                product("Squat Rack Pro", "Heavy-gauge uprights with safety arms and plate storage.", "18999", 6, "Racks", unsplash("photo-1577992805669-c80be3285f36")),
                product("Power Cage Station", "Full power cage with pull-up bar and J-hooks.", "27999", 4, "Racks", unsplash("photo-1687350119840-3e2cc5977e92")),
                // Home gym
                product("Multi-Function Home Gym", "Cable-based home station for full-body training.", "34999", 5, "Home Gym", unsplash("photo-1722248211551-68fa9e63ad4e")),
                product("Compact Home Gym Station", "Foldable strength station built for small spaces.", "15999", 7, "Home Gym", "/images/products/home-gym.svg"),
                // Strength / recovery / accessories / cardio (newest ids last:
                // the storefront "New arrivals" section reads the highest ids)
                product("Indoor Cycling Bike", "A compact cardio setup for steady rides at home.", "24999", 4, "Cardio", unsplash("photo-1761971976282-b2bb051a5474")),
                product("Compact Stepper", "Low-impact cardio training with a small footprint.", "7999", 9, "Cardio", "/images/products/cardio.svg"),
                product("Skipping Rope Timer", "Interval timer for HIIT and circuit sessions.", "1499", 3, "Cardio", "/images/products/cardio.svg"),
                product("Foam Recovery Roller", "Firm, textured support for warmups and post-session recovery.", "999", 20, "Recovery", "/images/products/recovery.svg"),
                product("Lifting Grip Pads", "Protective grips for deadlifts and heavy pulls.", "599", 50, "Accessories", "/images/products/accessories.svg"),
                product("Resistance Band Kit", "Five resistance levels for warmups and full-body training.", "899", 42, "Accessories", unsplash("photo-1530021356476-0a6375ffe73b")),
                product("Training Jump Rope", "Lightweight speed rope for conditioning anywhere.", "699", 32, "Accessories", unsplash("photo-1758875568447-aa45a5d3b351")),
                product("Core Stability Mat", "Dense, grippy cushioning for floor work and mobility.", "1299", 28, "Recovery", unsplash("photo-1754839004013-0276d56bc0e7")),
                product("Cast Iron Kettlebell 16kg", "A durable training staple for swings, carries, and squats.", "2499", 7, "Strength", unsplash("photo-1570440696486-5cfc676e2a68"))
        ));
        log.info("Seeded demo product catalog");
    }

    private static String unsplash(String photoId) {
        return "https://images.unsplash.com/" + photoId + "?auto=format&fit=crop&w=900&q=80";
    }

    private Product product(String name, String description, String price, int stock,
                            String category, String imageUrl) {
        return new Product(name, description, new BigDecimal(price), stock, category, imageUrl);
    }
}

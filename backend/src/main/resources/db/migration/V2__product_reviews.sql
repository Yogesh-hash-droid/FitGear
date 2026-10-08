-- Product reviews: a customer may rate a product 1-5 stars and leave one
-- review per product (enforced by the unique key; posting again updates it).
-- Written in SQL that runs on both MySQL 8 and H2 (MySQL compatibility mode).

CREATE TABLE product_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    title VARCHAR(120),
    body VARCHAR(2000),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT uk_reviews_user_product UNIQUE (user_id, product_id),
    CONSTRAINT ck_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_reviews_product ON product_reviews (product_id);
CREATE INDEX idx_reviews_rating ON product_reviews (rating);

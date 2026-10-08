package com.fitgear.backend.security;

import com.fitgear.backend.auth.AppUser;
import com.fitgear.backend.auth.UserRepository;
import com.fitgear.backend.auth.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API acceptance test running against the real Spring context,
 * security filter chain, Flyway schema and H2 database.
 *
 * <p>Covers the required security matrix (401/403/404) and the complete
 * CUSTOMER + ADMIN order lifecycle with COD payment.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private String adminEmail;
    private String customerEmail;

    @BeforeEach
    void resetDatabase() {
        // Respect FK order: reviews -> cart -> order_items -> payments -> orders -> products -> users
        jdbcTemplate.update("DELETE FROM product_reviews");
        jdbcTemplate.update("DELETE FROM cart_items");
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM users");

        adminEmail = "admin-flow@fitgear.test";
        customerEmail = "customer-flow@fitgear.test";
        userRepository.save(new AppUser("Flow Admin", adminEmail, passwordEncoder.encode("AdminPass123!"), UserRole.ADMIN));
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/payments/config")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicCatalogIsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Guest","email":"guest-public@fitgear.test","password":"Password123"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    void customerCannotAccessAdminEndpoints() throws Exception {
        registerAndLogin("role-customer@fitgear.test", "Password123");
        String token = login("role-customer@fitgear.test", "Password123");

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/orders").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Sneaky Product", "100", 5)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotReadAnotherCustomersOrder() throws Exception {
        String adminToken = login(adminEmail, "AdminPass123!");
        String ownerToken = registerAndLogin("order-owner@fitgear.test", "Password123");
        String intruderToken = registerAndLogin("order-intruder@fitgear.test", "Password123");

        long productId = createProduct(adminToken, "Owner Product", "1000", 5);
        long orderId = placeCodOrder(ownerToken, productId, 1);

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(intruderToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel").header("Authorization", bearer(intruderToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrationAlwaysCreatesCustomerRole() throws Exception {
        String token = registerAndLogin("plain-user@fitgear.test", "Password123");

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void completeOrderLifecycleWithCod() throws Exception {
        String adminToken = login(adminEmail, "AdminPass123!");
        String customerToken = registerAndLogin(customerEmail, "Password123");

        // 1. Admin creates a product with stock
        long productId = createProduct(adminToken, "Pro Squat Rack", "2999", 5);
        assertThat(productId).isPositive();

        // 2. Customer browses and adds it to the cart
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(5));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":2}"))
                .andExpect(status().isCreated());

        // 3. Server computes the cart subtotal
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.subtotal").value(5998.0));

        // 4. Checkout with COD (cart already holds the 2 units added in step 2)
        long orderId = placeOrder(customerToken);
        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.payment.method").value("COD"))
                .andExpect(jsonPath("$.address.city").value("Chennai"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.cancelEligible").value(true));

        // 5. Stock was reserved (5 - 2 = 3)
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(jsonPath("$.stock").value(3));

        // 6. Cart was cleared
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(customerToken)))
                .andExpect(jsonPath("$.items.length()").value(0));

        // 7. Admin advances: CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED
        updateStatus(adminToken, orderId, "PROCESSING");
        updateStatus(adminToken, orderId, "SHIPPED");
        updateStatus(adminToken, orderId, "DELIVERED");

        // 8. COD payment captured on delivery, no longer cancellable
        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(customerToken)))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.payment.status").value("PAID"))
                .andExpect(jsonPath("$.cancelEligible").value(false));

        // 9. Invalid backward transition rejected with 409
        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancellingRestoresStock() throws Exception {
        String adminToken = login(adminEmail, "AdminPass123!");
        String customerToken = registerAndLogin("cancel-user@fitgear.test", "Password123");

        long productId = createProduct(adminToken, "Cancel Bench", "1500", 4);
        long orderId = placeCodOrder(customerToken, productId, 3);

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(jsonPath("$.stock").value(1));

        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.payment.status").value("CANCELLED"));

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(jsonPath("$.stock").value(4));
    }

    @Test
    void razorpayCheckoutReportsNotConfiguredWithoutKeys() throws Exception {
        String adminToken = login(adminEmail, "AdminPass123!");
        String customerToken = registerAndLogin("cod-only@fitgear.test", "Password123");

        mockMvc.perform(get("/api/payments/config").header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayEnabled").value(false));

        long productId = createProduct(adminToken, "COD Only", "500", 2);
        long orderId = placeCodOrder(customerToken, productId, 1);
        mockMvc.perform(post("/api/payments/checkout")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + "}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void adminDashboardReturnsMetrics() throws Exception {
        String adminToken = login(adminEmail, "AdminPass123!");
        String customerToken = registerAndLogin("metrics-user@fitgear.test", "Password123");
        long productId = createProduct(adminToken, "Metric Product", "800", 10);
        placeCodOrder(customerToken, productId, 2);

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalProducts").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalRevenue").value(org.hamcrest.Matchers.greaterThan(0.0)))
                .andExpect(jsonPath("$.ordersByStatus.CONFIRMED").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.recentOrders.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ------------------------------------------------------------------
    // product reviews
    // ------------------------------------------------------------------

    @Test
    void reviewReadsArePublicButWritesRequireSignIn() throws Exception {
        String adminToken = login(adminEmail, "AdminPass123!");
        long productId = createProduct(adminToken, "Public Review Rack", "2000", 3);

        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products/" + productId + "/reviews/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(0))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.distribution['5']").value(0));

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reviews/mine?productId=" + productId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/reviews/1"))
                .andExpect(status().isUnauthorized());

        // Public reads still 404 for a product that does not exist
        mockMvc.perform(get("/api/products/999999/reviews"))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerCanCreateUpdateReadAndDeleteTheirOwnReview() throws Exception {
        String adminToken = login(adminEmail, "AdminPass123!");
        String token = registerAndLogin("reviewer@fitgear.test", "Password123");
        long productId = createProduct(adminToken, "Reviewed Bench", "1500", 4);

        mockMvc.perform(get("/api/reviews/mine?productId=" + productId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        long reviewId = createReview(token, productId, 4, "Solid", "Stable under load.");

        // Posting again updates the same row rather than adding a second review
        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"title\":\"Revised\",\"body\":\"Even better after a month.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.title").value("Revised"));

        mockMvc.perform(get("/api/products/" + productId + "/reviews")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.content[0].userName").value("Flow User"));

        mockMvc.perform(get("/api/reviews/mine?productId=" + productId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));

        mockMvc.perform(get("/api/products/" + productId + "/reviews/summary")
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.totalReviews").value(1))
                .andExpect(jsonPath("$.distribution['5']").value(1))
                .andExpect(jsonPath("$.distribution['1']").value(0));

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void customerCannotDeleteAnotherCustomersReview() throws Exception {
        String adminToken = login(adminEmail, "AdminPass123!");
        String ownerToken = registerAndLogin("review-owner@fitgear.test", "Password123");
        String intruderToken = registerAndLogin("review-intruder@fitgear.test", "Password123");
        long productId = createProduct(adminToken, "Contested Rack", "2500", 2);

        long reviewId = createReview(ownerToken, productId, 3, "Mine", "Owner review.");

        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", bearer(intruderToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // The owner still can
        mockMvc.perform(delete("/api/reviews/" + reviewId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void reviewValidationRejectsRatingOutsideRange() throws Exception {
        String adminToken = login(adminEmail, "AdminPass123!");
        String token = registerAndLogin("review-validate@fitgear.test", "Password123");
        long productId = createProduct(adminToken, "Validated Bench", "900", 7);

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rating").exists());

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":null}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rating").value("Rating must be between 1 and 5"));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private long createReview(String token, long productId, int rating, String title, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":" + rating
                                + ",\"title\":\"" + title + "\",\"body\":\"" + body + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
    private String registerAndLogin(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Flow User\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
        return login(email, password);
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    private String productJson(String name, String price, int stock) {
        return "{\"name\":\"" + name + "\",\"description\":\"Integration test product\","
                + "\"price\":" + price + ",\"stock\":" + stock + ",\"category\":\"Strength\"}";
    }

    private long createProduct(String adminToken, String name, String price, int stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(name, price, stock)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void addToCart(String customerToken, long productId, int quantity) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"))
                .andExpect(status().isCreated());
    }

    private long placeCodOrder(String customerToken, long productId, int quantity) throws Exception {
        addToCart(customerToken, productId, quantity);
        return placeOrder(customerToken);
    }

    private long placeOrder(String customerToken) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "address", Map.of(
                        "receiverName", "Flow Customer",
                        "phone", "9876543210",
                        "line1", "12 Temple Street",
                        "city", "Chennai",
                        "state", "Tamil Nadu",
                        "pincode", "600001"
                ),
                "paymentMethod", "COD"
        ));

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void updateStatus(String adminToken, long orderId, String status) throws Exception {
        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status));
    }
}

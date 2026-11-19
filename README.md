# FitGear — Professional Full-Stack E-Commerce

A complete fitness-equipment store: browsable catalog, server-side cart, checkout with
**Razorpay + Cash on Delivery**, full **order lifecycle** with admin fulfilment,
**live order status over SSE**, role-based access control, and a Docker Compose deployment.

```
┌─────────────┐   same-origin /api    ┌────────────────┐      ┌────────────┐
│  React SPA  │ ────────────────────► │  nginx (Docker)│ ───► │ Spring Boot│
│  Vite/JS    │   (dev: :5173→:8080)  │  static+proxy  │      │  :8080     │
└─────────────┘                       └────────────────┘      └─────┬──────┘
     │  SSE /api/notifications/stream                               │
     │  ◄───────────────────────────────────────────────────────────┤
     │                                                              ▼
     │            ┌──────────────┐  webhook  ┌──────────┐    ┌────────────┐
     └──────────► │  Razorpay    │ ────────► │ backend  │    │ MySQL/H2   │
                   │  checkout.js │           │ /webhook │    │ + Flyway   │
                   └──────────────┘           └──────────┘    └────────────┘
```

## Tech stack

| Layer    | Technology |
|----------|------------|
| Backend  | Java 21, Spring Boot 4.0 (Web, Security, Data JPA, Validation, Actuator), Flyway, jjwt, Razorpay SDK |
| Frontend | React 19, Vite, react-router-dom 7, axios, plain JS + CSS (no TypeScript) |
| Database | H2 in-memory, MySQL-compatible mode (dev) · MySQL 8.4 (Docker) |
| Realtime | Server-Sent Events (fetch-based client; single connection per user) |
| Payments | Razorpay (HMAC-SHA256 verified, webhook support) + Cash on Delivery |
| Deploy   | Docker multi-stage builds, nginx reverse proxy, Docker Compose |

## Repository layout

```
FitGear/
├── backend/                 Spring Boot app (Maven)
│   ├── src/main/java/com/fitgear/backend/
│   │   ├── auth/            register, login, profile (JWT)
│   │   ├── product/         catalog, search/filter/sort, categories, seeder
│   │   ├── review/          product ratings (1-5), one review per customer
│   │   ├── cart/            server-side cart with computed totals
│   │   ├── order/           placement, lifecycle transitions, cancellation
│   │   ├── payment/         Razorpay + COD, signature verification, webhook
│   │   ├── notification/    SSE hub (live order status)
│   │   ├── admin/           dashboard metrics
│   │   ├── security/        JWT filter, SecurityConfig, RBAC rules
│   │   ├── config/          CORS, admin provisioning
│   │   └── exception/       global error handler
│   └── src/main/resources/db/migration/    ← schema source of truth (V1, V2)
├── frontend/                React SPA (Vite)
│   └── src/
│       ├── pages/           14 routes (store, checkout, orders, admin, 404)
│       ├── components/      Navbar, Footer, ProductCard, OrderTimeline, StatusBadge,
│       │                    StarRating, ReviewSummary, ReviewForm, ProductReviews
│       ├── context/         Auth, Cart (guest+server), Toast
│       ├── services/        api.js (axios + 401 interceptor), sse.js (SSE client)
│       └── utils/format.js  price/date/status helpers, transition map
├── docker-compose.yml       mysql + backend + frontend (nginx)
├── .env.example             all configuration, copy to .env
└── README.md                ← you are here
```

## Quick start

### Prerequisites

- Java 21+, Maven (or use `mvnw`)
- Node 20+ / 22
- Docker + Docker Compose (for containerised run)

### A. Local development (recommended while studying)

```bash
# 1. Backend — http://localhost:8080
cd backend
set JWT_SECRET=dev-secret-please-change-32-characters-minimum
set ADMIN_EMAIL=admin@fitgear.local
set ADMIN_PASSWORD=Admin@FitGear2026
mvnw spring-boot:run          # Windows: mvnw.cmd / macOS: ./mvnw

# 2. Frontend — http://localhost:5173 (new shell)
cd frontend
npm install
npm run dev
```

The dev database is in-memory H2 (Flyway-migrated on startup) and 21 demo products are
seeded automatically (`SEED_PRODUCTS=false` to skip).

### B. Docker Compose (production-shaped)

```bash
cp .env.example .env          # then edit JWT_SECRET + ADMIN_PASSWORD + MYSQL_PASSWORD
docker compose up --build
# App:    http://localhost
# API:    http://localhost:8080/actuator/health
```

> Docker was not available on the authoring machine, so the compose files are
> written to spec but were not executed here — see *Limitations*.

## Configuration (`.env`)

| Variable | Required | Purpose |
|----------|----------|---------|
| `JWT_SECRET` | **yes** | HMAC key, ≥ 32 chars, **no defaults in source** |
| `JWT_EXPIRATION` | no | token lifetime, default `PT12H` |
| `CORS_ALLOWED_ORIGINS` | no | comma-separated allow-list (default `http://localhost:5173`) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | admin seeded on startup | bootstrap admin; public registration is always `CUSTOMER` |
| `MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD` | for mysql profile | JDBC connection |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` / `RAZORPAY_WEBHOOK_SECRET` | no | leave empty → COD-only checkout |
| `SEED_PRODUCTS` | no | seed demo catalog on empty DB (default `true`) |
| `SPRING_PROFILES_ACTIVE` | prod | `mysql` = production-shaped profile (env-driven MySQL, `server.port=${PORT:8080}`) |
| `DB_POOL_MAX_SIZE` / `DB_POOL_MIN_IDLE` | no | Hikari pool bounds on the `mysql` profile (defaults 10 / 2) |
| `VITE_API_BASE_URL` | **yes (Vercel)** | deployed backend API base, e.g. `https://YOUR-RENDER-BACKEND-URL.onrender.com/api` |
| `VITE_API_URL` | no | legacy alias for the API base (still honoured) |

## Deployment

Production architecture: **Vercel (React) → Render (Spring Boot) → Aiven (MySQL)**.
Step-by-step dashboard instructions, environment variables, Aiven TLS setup,
Razorpay test-mode/webhook configuration, SSE notes and troubleshooting live in
[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md).

## API reference

All endpoints are under `/api`. Roles: **P** = public, **C** = authenticated customer, **A** = admin.

### Auth
| Method | Path | Role | Notes |
|--------|------|------|-------|
| POST | `/auth/register` | P | creates `CUSTOMER` (name, email, password ≥ 8) |
| POST | `/auth/login` | P | → `{token, userId, name, email, role}` |
| GET | `/auth/me` | C | profile |
| PUT | `/auth/me` | C | update name |

### Catalog
| Method | Path | Role | Notes |
|--------|------|------|-------|
| GET | `/products` | P | `search, category, minPrice, maxPrice, page, size, sort` → Spring `Page` |
| GET | `/products/{id}` | P | |
| POST/PUT/DELETE | `/products[/{id}]` | A | validation: name ≤ 120, price > 0, stock ≥ 0, image ≤ 2048 |
| GET | `/categories` | P | distinct category names |

### Reviews (one per customer per product — posting again updates it)
| Method | Path | Role | Notes |
|--------|------|------|-------|
| GET | `/products/{id}/reviews` | P | `page, size, sort` (`createdAt`·`rating`·`id`) → Spring `Page` |
| GET | `/products/{id}/reviews/summary` | P | `{averageRating, totalReviews, distribution{5..1}}` — always all five buckets |
| GET | `/reviews/mine?productId=` | C | own review, **204** when none |
| POST | `/products/{id}/reviews` | C | `{rating: 1-5, title?, body?}` → upserts the caller's review |
| DELETE | `/reviews/{reviewId}` | C/A | owner only, unless admin; **403** otherwise |

### Cart (server-computed totals — prices never accepted from the client)
| Method | Path | Role |
|--------|------|------|
| GET | `/cart` | C | `{items, itemCount, subtotal}` |
| POST | `/cart/items` | C | `{productId, quantity}` |
| PUT | `/cart/items/{itemId}` | C | `{quantity}` |
| DELETE | `/cart/items/{itemId}` · `/cart` | C |

### Orders
| Method | Path | Role | Notes |
|--------|------|------|-------|
| POST | `/orders` | C | `{address, paymentMethod: RAZORPAY\|COD}`; stock decremented under pessimistic locks |
| GET | `/orders` · `/orders/{id}` | C | own orders only |
| PATCH | `/orders/{id}/cancel` | C | pre-`SHIPPED` only; restores stock; PAID → `REFUNDED` |

### Payments
| Method | Path | Role | Notes |
|--------|------|------|-------|
| GET | `/payments/config` | C | `{razorpayEnabled, keyId}` — checkout hides Razorpay when keys are absent |
| POST | `/payments/checkout` | C | `{orderId}` → Razorpay order (`amountPaise`) |
| POST | `/payments/verify` | C | HMAC-SHA256 signature verified **server-side**, constant-time compare |
| POST | `/payments/webhook` | P | authenticated by `X-Razorpay-Signature`, never by the client |

### Admin
| Method | Path | Role |
|--------|------|------|
| GET | `/admin/dashboard` | A | orders, revenue, per-status counts, products, low stock, customers, recent orders |
| GET | `/admin/orders?status=&page=&size=` | A | paged |
| GET | `/admin/orders/{id}` | A | |
| PATCH | `/admin/orders/{id}/status` | A | `{status}` — validated transition map, **409** on illegal moves |

### Realtime
| Method | Path | Role |
|--------|------|------|
| GET | `/notifications/stream` | C | `text/event-stream`, event `order-event`, heartbeat every 25 s, 5 min timeout |

Security rules: everything else under `/api/**` needs a valid JWT; failures → 401,
insufficient role → 403. All validation/business errors return the same JSON envelope
`{status, message}` from the global handler.

## Data model (Flyway `V1__init.sql`, `V2__product_reviews.sql`)

```
users(id, name, email UQ, password, role, created_at)
products(id, name, description, price, stock, category, image_url, created_at, updated_at)
cart_items(id, user_id FK, product_id FK, quantity, UQ(user,product))
orders(id, user_id FK, status, subtotal, total,
       receiver_name, phone, address_line1/2, city, state, pincode,
       created_at, updated_at)
order_items(id, order_id FK, product_id, product_name, image_url, unit_price, quantity)
payments(id, order_id FK, method RAZORPAY|COD, status PENDING|PAID|FAILED|REFUNDED|CANCELLED,
         razorpay_order_id, razorpay_payment_id, amount, created_at)
product_reviews(id, product_id FK ON DELETE CASCADE, user_id FK, rating 1-5, title, body,
                created_at, updated_at, UQ(user_id, product_id), CHECK(rating BETWEEN 1 AND 5))
```

Prices are `DECIMAL(19,2)`; order items snapshot name/price/image at purchase time.
Hibernate runs with `ddl-auto=validate` — **Flyway owns the schema**.

## Order lifecycle

```
PLACED ──► CONFIRMED ──► PROCESSING ──► SHIPPED ──► DELIVERED
   │            │              │
   └────────────┴──────────────┴──► CANCELLED  (allowed while cancelEligible;
                                               restores stock, refunds if PAID)
```

- **COD**: order is created `CONFIRMED`, payment `PENDING` → marked `PAID` when admin delivers.
- **Razorpay**: order is created `PLACED`, payment `PENDING` → signature verification
  (or webhook) marks `PAID` and moves the order to `CONFIRMED`.
- Illegal transitions return **409 CONFLICT** (`InvalidOrderTransitionException`).
- Every status change pushes an `ORDER_STATUS_CHANGED` SSE event `{orderId, status, message}`.

## Frontend routes

`/` home · `/products` (search/category/price/sort/pagination) · `/products/:id` ·
`/login` `/register` · `/cart` (guest cart in localStorage, merged at sign-in) ·
`/checkout` (address → payment method → Razorpay modal or COD) · `/orders` ·
`/orders/:id` (live timeline via SSE) · `/profile` · `/admin` · `/admin/products` ·
`/admin/orders` · `*` → 404.

Notable mechanics:
- **One SSE connection** for the whole app (wired in `App.jsx`), using `fetch` because
  `EventSource` cannot send an `Authorization` header; auto-reconnects with backoff.
- **401 interceptor** signs the user out and redirects to `/login`.
- Server is the source of truth for cart totals and order amounts.

## Testing

```bash
cd backend && mvnw test        # 63 tests (services + MockMvc integration)
cd frontend && npm run lint && npm run build
```

Coverage includes: auth + RBAC (401/403 matrix), cart & stock validation, order
placement/cancellation (stock restore, refund state), payment signature accept/reject,
lifecycle transition rules, admin dashboard, SSE hub behaviour, review upsert +
ownership + rating validation, and a full register→login→cart→order security-flow
integration test.

## Limitations

- **Docker not verified locally** — Docker isn't installed on this machine; compose files are written but untested (backend/frontend builds were verified natively).
- SSE hub is single-node by design; horizontal scaling would move it behind a shared broker (e.g. Redis pub/sub).
- COD refunds are recorded as `REFUNDED` state only — no payment-gateway refund API call.
- No email/SMS notifications, wishlist, or coupon engine.

## Study guide — where to read first

| To understand… | Read |
|----------------|------|
| Security rules & roles | `backend/.../security/SecurityConfig.java` |
| JWT issuing/parsing | `backend/.../auth/JwtService.java` |
| Transactional order placement | `backend/.../order/OrderService.java` |
| Payments & signatures | `backend/.../payment/PaymentService.java`, `util/SignatureUtils.java` |
| SSE hub | `backend/.../notification/NotificationService.java` + `frontend/src/services/sse.js` |
| Error handling | `backend/.../exception/GlobalExceptionHandler.java` |
| Schema | `backend/src/main/resources/db/migration/V1__init.sql`, `V2__product_reviews.sql` |
| Reviews & aggregate rating | `backend/.../review/ReviewService.java` + `frontend/src/components/ProductReviews.jsx` |
| Full API contract test | `backend/src/test/.../security/SecurityFlowIntegrationTest.java` |
| Frontend data layer | `frontend/src/services/api.js`, `context/CartContext.jsx` |
| Checkout UX | `frontend/src/pages/CheckoutPage.jsx` |

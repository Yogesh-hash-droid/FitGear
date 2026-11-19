# FitGear — Deployment Guide (Vercel + Render + Aiven MySQL)

This guide takes you from the finished codebase to a live deployment, step by step.
Every dashboard setting names the **exact field** and the **value to enter**.

> **Golden rules**
>
> 1. Never commit `.env`, passwords, JWT secrets, Razorpay keys, or the Aiven CA file — `.gitignore` already blocks them.
> 2. Nothing is pushed to GitHub and no paid resource is created without your explicit approval.
> 3. Placeholders like `YOUR-RENDER-BACKEND-URL` are yours to replace — this guide never invents credentials or URLs.

**Deployment order** (one round-trip is needed for CORS, explained in §14):

```
① Push code to GitHub  →  ② Create Aiven MySQL  →  ③ Deploy backend on Render
→  ④ Deploy frontend on Vercel  →  ⑤ Put the Vercel URL into Render's CORS variable
→  ⑥ Razorpay test keys + webhook  →  ⑦ Run the test checklists (§18–§21)
```

---

## 1. FitGear deployment architecture

```
 Browser (customer)
    │
    │  1. loads the React app (static files)
    ▼
 ┌──────────────────────┐
 │  VERCEL             │  frontend/  (React 19 + Vite)
 │  YOUR-VERCEL-URL    │
 └──────────┬──────────┘
            │  2. API calls:  https://YOUR-RENDER-BACKEND-URL.onrender.com/api/...
            │     (Authorization: Bearer <JWT>, CORS-checked by the backend)
            ▼
 ┌──────────────────────┐        ┌──────────────────────────┐
 │  RENDER              │  3.    │  AIVEN MySQL             │
 │  Spring Boot (Docker)│ ─────► │  TLS (sslMode=VERIFY_CA) │
 │  :PORT (default10000)│  JDBC  │  Flyway migrations       │
 └──────────┬──────────┘        └──────────────────────────┘
            │
            │  4. HTTPS (server-side only, secrets never reach the browser)
            ▼
      Razorpay API  ◄──── webhook: POST /api/payments/webhook ──── Razorpay
```

* **SSE (live order updates)** is a long-lived HTTP stream from Render straight to the browser, authenticated with the JWT.
* **Razorpay checkout.js** loads in the browser directly from Razorpay; the *secret* key stays on Render.
* Local development is unchanged: H2 in-memory DB + `http://localhost:5173` + `http://localhost:8080`.

## 2. Required hosting accounts

| Account | Used for | Status |
|---|---|---|
| **GitHub** | source code, auto-deploys | needed |
| **Aiven** (console.aiven.io) | production MySQL | **you already have it** |
| **Render** (dashboard.render.com) | Spring Boot backend | sign up (free plan exists) |
| **Vercel** (vercel.com) | React frontend | sign up (free plan exists) |
| **Razorpay** (dashboard.razorpay.com) | payments (test mode = free) | optional at first (COD works without it) |

No paid plan is required to complete this guide. See §26 for verified free-tier limitations.

## 3. Create the Aiven MySQL service

Exact steps from Aiven's official "Get started" documentation:

1. Log in to the **Aiven Console** → open your project.
2. In the left sidebar click **Services**.
3. Click **Create service**.
4. Select **MySQL**.
5. **Cloud** field → choose a cloud provider (for example AWS, GCP, DigitalOcean — whichever offers a region near your users).
6. **Plan** field → choose a plan (plans vary between clouds/regions; start with the cheapest available plan for testing).
7. **Service details** → **Service name** field → enter e.g. `fitgear-mysql`.
8. Optional: add service tags.
9. **Service summary** → click **Create service**.
10. Wait while the status shows **Rebuilding**. When it becomes **Running** (a few minutes) the service is usable.

## 4. Selecting plan and region

* **Region**: pick a region close to your Render region (and your users). Every 1000 km adds round-trip latency to every query. Example: Render `Frankfurt` ↔ Aiven `Frankfurt`/nearby is ideal; mismatched continents work but are slower.
* **Plan**: the cheapest plan is fine to start. Two plan-dependent things to know:
  * **Backup retention** depends on your plan (daily full backups are automatic — see §22).
  * **Connection limits** depend on your plan — keep the app's pool (`DB_POOL_MAX_SIZE`, default 10) well below your plan's `max_connections`.
* Check current prices: aiven.io/pricing and render.com/pricing (this guide does not quote prices — they change).

## 5. Obtain host, port, database name, username and password

All of it lives in one place:

1. Open your service in the **Aiven Console**.
2. Go to the **Overview** page of the service.
3. Find the **Connection information** section. It contains:
   * **Host** — e.g. `something.f.aivencloud.com` (copy it exactly)
   * **Port** — a per-service number (copy it; do not assume 3306)
   * **Database** — the default database is `defaultdb` (FitGear's Flyway migrations will create all tables inside it)
   * **Username** — `avnadmin`
   * **Password** — the password for the `avnadmin` user (copy it now and store it in a password manager; you can reset it later from the console if lost)
4. Alternatively click the **Quick connect** button on the same page for ready-made connection strings.

Command-line alternative (official):

```bash
avn service user-creds-download --username avnadmin <service-name>
```

> The `avnadmin` user can create databases too, but FitGear does not need that — it uses the default database directly.

## 6. Download the Aiven CA certificate

The backend verifies the MySQL server's TLS certificate against Aiven's project CA. To get it:

1. Aiven Console → your service → **Overview** page.
2. In the **Connection information** section find **CA Certificate**.
3. Click **Download** — you get a PEM file (plain text, starts with `-----BEGIN CERTIFICATE-----`).

Notes (from Aiven's official TLS documentation):

* MySQL connections using `VERIFY_CA` or `VERIFY_IDENTITY` **require** this CA. FitGear uses `VERIFY_CA`.
* **CA rotation**: Aiven periodically rotates the project CA. During a rotation the downloaded file contains **two** certificates. FitGear's installer reads *all* certificates in the file automatically, so a rotated bundle keeps working — but you must re-download and update the Render secret file before Aiven's second maintenance update ("Scheduled maintenance for TLS certificate update"). Aiven emails project contacts when this happens.
* Although a CA certificate is public material, this project's rules are strict: **never commit it** — it goes only into Render's secret-file field.

## 7. Configure SSL/TLS securely (how FitGear does it)

FitGear ships a small startup component, `AivenTrustStoreInstaller`, that makes TLS verification work on Render without any insecure bypass:

1. At backend startup it looks for the PEM at **`/etc/secrets/aiven-ca.pem`** (override with the env var `AIVEN_CA_PEM_PATH`).
2. If found, it converts the PEM into a PKCS12 truststore in memory/temp storage, **merging it with the JVM's own public trust anchors** (so Razorpay's HTTPS keeps working), and points the JVM at it.
3. MySQL then connects with **`sslMode=VERIFY_CA`**: the Aiven server certificate must be signed by the Aiven CA, or the connection is refused (**fail closed** — a broken/missing CA makes the app refuse to start rather than connect insecurely).
4. In local development the PEM file doesn't exist → the installer is a no-op and nothing changes.

The production JDBC URL is therefore simply:

```
jdbc:mysql://AIVEN-HOST:AIVEN-PORT/defaultdb?sslMode=VERIFY_CA&serverTimezone=UTC
```

**Never** use `useSSL=false`, `sslMode=DISABLED`, or `sslMode=REQUIRED` alone in production — they skip certificate verification. (`sslMode=VERIFY_IDENTITY` is even stricter — it also checks the hostname; you can switch to it later if you verify it works with your Aiven hostname.)

## 8. Configure Render to connect to Aiven

Render receives three pieces (set in §10):

| Piece | Where it goes | Value |
|---|---|---|
| JDBC URL (host/port/db + `sslMode=VERIFY_CA`) | env var `MYSQL_URL` | from §5 + §7 |
| `avnadmin` username/password | env vars `MYSQL_USERNAME` / `MYSQL_PASSWORD` | from §5 |
| Aiven CA (PEM) | **Secret File** named `aiven-ca.pem` | contents of the download from §6 |

The app automatically reads the secret file from `/etc/secrets/aiven-ca.pem` (Render's standard path for Docker services) and installs it as the truststore. The Docker image already grants its runtime user access to that path.

## 9. Render environment variables (complete list)

In the Render Dashboard these are added under the service's **Environment** section as key/value rows. Mark **MYSQL_PASSWORD**, **JWT_SECRET**, **ADMIN_PASSWORD** and the Razorpay values as *Secret* type.

| Key | Value to enter | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `mysql` | activates the production profile (already the image default; set it explicitly anyway) |
| `MYSQL_URL` | `jdbc:mysql://AIVEN-HOST:AIVEN-PORT/defaultdb?sslMode=VERIFY_CA&serverTimezone=UTC` | replace HOST/PORT from §5; keep `sslMode=VERIFY_CA` |
| `MYSQL_USERNAME` | `avnadmin` | from §5 |
| `MYSQL_PASSWORD` | *(your avnadmin password)* | Secret — from §5 |
| `JWT_SECRET` | *(generate one, see below)* | Secret, **required**, minimum 32 characters — the app refuses to start without it |
| `JWT_EXPIRATION` | `PT12H` | optional; ISO-8601 token lifetime (12 hours) |
| `CORS_ALLOWED_ORIGINS` | `https://YOUR-VERCEL-URL.vercel.app` | exact Vercel origin, `https://`, **no trailing slash**; comma-separate multiple origins (§14) |
| `ADMIN_EMAIL` | `you@example.com` | first-run admin bootstrap only |
| `ADMIN_PASSWORD` | *(strong password)* | Secret. **Existing admins are never reset** — the account is only created if it doesn't exist yet |
| `RAZORPAY_KEY_ID` | `rzp_test_...` | Secret, optional (§15). Empty → COD-only checkout |
| `RAZORPAY_KEY_SECRET` | *(test key secret)* | Secret, backend only — never exposed to the browser |
| `RAZORPAY_WEBHOOK_SECRET` | *(your webhook secret)* | Secret, must match what you enter in Razorpay (§16) |
| `SEED_PRODUCTS` | `true` | seeds the demo catalog **only when the product table is empty**; never overwrites. Set `false` if you don't want demo products |
| `DB_POOL_MAX_SIZE` | `10` | optional; Hikari connection pool cap (keep below your plan's `max_connections`) |
| `AIVEN_CA_PEM_PATH` | *(leave unset)* | optional; defaults to `/etc/secrets/aiven-ca.pem` |

Generate a strong `JWT_SECRET` in PowerShell (64 random characters):

```powershell
-join ((1..64) | ForEach-Object { '{0:x}' -f (Get-Random -Maximum 16) })
```

Do **not** set `PORT` — Render injects it (default `10000`) and the app binds to it automatically. Never put these values in the Dockerfile, `docker-compose.yml`, or any file in Git.

## 10. Deploy the Spring Boot backend on Render

Prerequisite: the code (including this guide) is pushed to GitHub — **your approval, step by step in §23**.

1. Render Dashboard → click **New** → **Web Service**.
2. Connect your GitHub repository (authorize Render if asked).
3. **Name** field → e.g. `fitgear-backend`.
4. **Region** field → pick one close to your Aiven region (§4).
5. **Language** field → select **Docker** (important — even though this is Java).
6. **Dockerfile Path** field → `backend/Dockerfile` (the path is relative to the repository root).
7. **Branch** field → your default branch (e.g. `master`).
8. **Instance Type** / compute plan → **Free** to start (read §26 first).
9. Expand the **Advanced** section and set:
   * **Health Check Path** field → `/actuator/health`
   * **Environment Variables** → add every row from §9
   * **Secret Files** → click **+ Add Secret File**:
     * **Filename** field → `aiven-ca.pem`
     * **Contents** field → paste the **entire** PEM file (open the download in Notepad; keep the `-----BEGIN CERTIFICATE-----` / `-----END CERTIFICATE-----` lines)
     * Click **Save Changes**
10. Click **Create Web Service**.
11. Open the **Logs** tab and watch for this sequence (build takes a few minutes the first time):
    * `Successfully applied 2 migrations ... now at version v2` ← Flyway created the schema (§17)
    * `Seeded demo product catalog` ← first-run seed (only on an empty DB)
    * `Installed Aiven CA (1 certificate(s)) merged with ... JVM trust anchor(s)` ← TLS trust installed
    * `Tomcat started on port 10000 (http)` ← bound to Render's `PORT`
    * `Started FitGearApplication`
12. After editing environment variables later, click **Save Changes**; if a redeploy doesn't start by itself, go to the **Deploys** page → **Manual Deploy** dropdown → **Deploy latest commit**.

## 11. Verify the backend health endpoint

Replace `YOUR-RENDER-BACKEND-URL` with your service's `onrender.com` address (shown on the service's **Overview** page):

```bash
# 1. Health check → expect JSON containing "status":"UP"
curl https://YOUR-RENDER-BACKEND-URL.onrender.com/actuator/health

# 2. Public catalog → expect 200 + product JSON
curl https://YOUR-RENDER-BACKEND-URL.onrender.com/api/products

# 3. Protected endpoint without a token → expect 401
curl -i https://YOUR-RENDER-BACKEND-URL.onrender.com/api/cart
```

Render also shows the health-check state on the service page (green = healthy). If health fails, the **Logs** tab has the exact reason.

## 12. Deploy the React frontend on Vercel

1. vercel.com → **Add New…** → **Project** → import the FitGear GitHub repository.
2. **Framework Preset** → Vercel auto-detects **Vite** (if asked, choose Vite).
3. **Root Directory** field → `frontend` *(if the field isn't shown during import, create the project first, then: **Settings → General → Root Directory** → `frontend` → **Save**). This is essential — the app lives in the `frontend/` subfolder.*
4. **Build Command** → `npm run build` (Vite preset default)
5. **Output Directory** → `dist` (Vite preset default)
6. **Install Command** → `npm ci` (uses the committed `package-lock.json`)
7. **Environment Variables** → add `VITE_API_BASE_URL` (§13)
8. Click **Deploy**.

**SPA routing is already handled**: `frontend/vercel.json` rewrites every unknown path to `index.html`, so refreshing on `/products/123`, `/orders/5`, or `/admin/orders` renders the app instead of a 404. React Router handles the rest.

## 13. Configure VITE_API_BASE_URL

| Field | Value |
|---|---|
| Key | `VITE_API_BASE_URL` (exact spelling) |
| Value | `https://YOUR-RENDER-BACKEND-URL.onrender.com/api` |

Rules:

* The value **ends with `/api`** — the frontend appends paths like `/products`, `/auth/login`.
* It is **baked into the JavaScript bundle at build time**. After changing it you must redeploy: **Deployments** page → open the latest deployment → **⋯ → Redeploy** (or push any commit).
* Vite variables are **public by design** — they appear in the shipped bundle. **Never** put secrets here (no JWT, no Razorpay secret, no DB password). The only variable FitGear reads is `VITE_API_BASE_URL`.
* Local development needs nothing: without it the dev server uses `http://localhost:8080/api` automatically.
* Verify: open the deployed site → DevTools → **Network** → every API request should go to your `onrender.com` host, not to `*.vercel.app/api`.

## 14. Configure production CORS

The backend only accepts browser requests whose `Origin` is allow-listed:

| Field (on Render) | Value |
|---|---|
| `CORS_ALLOWED_ORIGINS` | `https://YOUR-VERCEL-URL.vercel.app` |

* Exact origin: scheme `https://`, host, **no trailing slash**, no path.
* Multiple origins → comma-separated (spaces are fine): `https://a.vercel.app,https://b.vercel.app`. Add Vercel preview URLs (`https://project-*.vercel.app`) only if you actually use them.
* After changing it: **Save Changes** → redeploy (§10 step 12).
* Local dev keeps working: the default is `http://localhost:5173` when the variable is unset.
* Covers every feature: login/registration, protected APIs, admin APIs, cart, orders, Razorpay endpoints, and the SSE stream (which needs `Authorization` and `Accept` allowed — both are).
* **Symptom of misconfiguration**: browser console shows *"...has been blocked by CORS policy"* — fix the variable (§24).

## 15. Configure Razorpay (TEST MODE only)

1. Razorpay Dashboard → **Settings** → **API Keys** → **Generate Test Key**.
2. You get a **Key ID** (`rzp_test_...`) and a **Key Secret** (shown once — store it in a password manager).
3. On Render set `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` (both Secret) → save → redeploy.
4. The backend creates Razorpay orders, verifies signatures server-side, and exposes only the Key ID to the browser (`GET /api/payments/config`). The **secret never leaves Render**.
5. Test payment (official Razorpay test cards, from their docs):
   * Visa `4100 2800 0000 1007` — random CVV, any future expiry
   * Mastercard `5555 5100 0008 1006` — random CVV, any future expiry
   * See razorpay.com/docs/payments/payments/test-card-details/ for more cards and failure-simulation cards.
6. No keys set → checkout automatically offers **Cash on Delivery** only (existing behavior, unchanged).

**Switching to live later (manual, never automatic):** **Settings → API Keys → Generate Live Key** → replace the two env vars on Render → update `RAZORPAY_WEBHOOK_SECRET` and the webhook URL (§16) → redeploy. Only do this when you're ready to accept real money.

## 16. Configure Razorpay webhooks

The webhook is the safety-net confirmation path (idempotent — duplicate notifications never double-process an order; invalid signatures are rejected).

1. Razorpay Dashboard → **Settings** → **Webhooks** → **Add New Webhook**.
2. Fields:
   * **Webhook URL** → `https://YOUR-RENDER-BACKEND-URL.onrender.com/api/payments/webhook`
   * **Secret** → create a random string **and enter the exact same value** as `RAZORPAY_WEBHOOK_SECRET` on Render (do this *before* the first test payment, or the signature check will reject the event)
   * Select events → `payment.captured` and `payment.failed`
3. Save. Razorpay can send a test ping — a `200 OK` response means the endpoint is reachable.

## 17. Verify Flyway migrations

On the **first** backend start against the empty Aiven database, the logs show:

```
Creating Schema History table "..." ...
Migrating schema "..." to version "1 - init"
Migrating schema "..." to version "2 - product reviews"
Successfully applied 2 migrations ... now at version v2
```

Verify from any MySQL client (DBeaver/MySQL Workbench, connection data from §5):

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

Expect **2 rows**, both `success = 1`.

On every later start you'll see `Schema ... is up to date. No migration necessary.` — migrations run **once, never twice**, and never drop or rewrite data. Hibernate runs with `ddl-auto=validate` — it only *checks* the schema matches the entities; it can never create, alter, or drop tables.

**Rules for the future:** never edit an applied migration file. Schema changes always go into a new file, e.g. `V3__something.sql` — Flyway applies it automatically on the next deploy.

**Initial seed data:** `SEED_PRODUCTS=true` inserts 21 demo products *only if the products table is empty* (`ProductCatalogInitializer` checks the count first). Once real products exist it never touches them. To start clean with your own catalog, set `SEED_PRODUCTS=false` before the first boot.

## 18. Test all customer features (checklist)

Open the deployed Vercel URL and verify:

- [ ] Home page loads, hero/products images render
- [ ] **Register** a new customer (password ≥ 8) → lands signed in
- [ ] **Logout** → **Login** again with the same credentials
- [ ] `/products` — search, category filter, price filter, sort, pagination
- [ ] Product detail page — images, price, stock, reviews, rating summary
- [ ] Add to cart (guest) → cart totals are server-computed
- [ ] Login with items in the guest cart → cart merges
- [ ] Cart: update quantity, remove item, stock-limit error appears
- [ ] **Checkout → Cash on Delivery** → order appears as `CONFIRMED` (COD flow)
- [ ] **Checkout → Razorpay** (test card from §15) → payment succeeds → order becomes `CONFIRMED`, payment `PAID`
- [ ] Failed/closed Razorpay widget → order stays unpaid, retry works
- [ ] `/orders` — history lists both orders; `/orders/:id` shows items + timeline
- [ ] Cancel an eligible order → status `CANCELLED`, stock restored
- [ ] Profile page — update name; refresh persists it (Aiven round-trip)
- [ ] Reviews: write a review (1–5 stars), edit it, delete it
- [ ] Signed-out user gets redirected to `/login` when opening `/orders` (401 interceptor works)

## 19. Test admin features (checklist)

- [ ] Log in with `ADMIN_EMAIL` / `ADMIN_PASSWORD` → `/admin` dashboard shows metrics (orders, revenue, low stock, recent orders)
- [ ] `/admin/products` — create, edit, delete a product; changes visible in the storefront
- [ ] `/admin/orders` — filter by status; open an order; move it through `CONFIRMED → PROCESSING → SHIPPED → DELIVERED`
- [ ] Illegal transition (e.g. backwards) → **409** with a friendly message
- [ ] **Security**: log in as a normal customer → `/admin` is blocked in the UI, and calling `/api/admin/dashboard` directly returns **403**
- [ ] Unauthenticated call to `/api/admin/orders` → **401**
- [ ] Register a fresh account → its role is `CUSTOMER` (public registration can never create admins)

## 20. Test order status SSE (live updates)

- [ ] Sign in as a customer in **browser A**, open `/orders/:id`
- [ ] Sign in as admin in **browser B** (or another machine), move the order to `SHIPPED`
- [ ] Browser A shows a toast and the timeline updates **without refreshing**
- [ ] Network tab: `GET /api/notifications/stream` with `Accept: text/event-stream` stays open; `connected` event arrives immediately; comment frames (`ping`) arrive every 25 s

How it works on this stack (unchanged architecture):

* The frontend uses a **fetch-based stream** (not `EventSource`) precisely so it can send the `Authorization: Bearer` header — this is required because Vercel and Render are different origins.
* Reconnects automatically with exponential backoff after any drop.
* **Render free plan**: the instance spins down after 15 minutes with no inbound traffic, which ends idle streams; the next request spins it back up in ~1 minute and the client reconnects by itself. On paid plans there is no spin-down.
* **Keep the backend single-instance** — the SSE hub is in-memory by design. Scaling to 2+ instances would require a shared broker (documented limitation, not a defect).

## 21. Test database persistence after a backend restart

1. Create data: a customer, a product (admin), an order.
2. Render → your service → **Deploys** page → **Manual Deploy** dropdown → **Deploy latest commit** (this fully restarts the app), or use Render's restart action if shown on your plan.
3. After it comes back:
   - [ ] The same customer can log in (users persisted)
   - [ ] The product and order are still there
   - [ ] Logs show `Schema ... is up to date. No migration necessary.` and **no** re-seeding (`Seeded demo product catalog` must **not** appear again)
4. This works because all data lives in **Aiven**, not on Render — Render's filesystem is ephemeral by design.

## 22. Back up and restore Aiven MySQL

**Automatic backups (built in, from Aiven's official docs):**

* A full backup is taken **every 24 hours**; binary logs are uploaded continuously so point-in-time recovery is possible.
* **How many backups are kept (retention) depends on your plan.**
* Backup schedule time can be changed: service → **Overview** → **Service settings** → **Advanced configuration** (option: backup time).

**Manual safety export (recommended after big changes):**

```bash
mysqldump --host AIVEN-HOST --port AIVEN-PORT --user avnadmin -p defaultdb > fitgear-backup-YYYY-MM-DD.sql
```

**Restore:** restores are performed from the Aiven Console — follow the official, plan-specific instructions at:
`docs.aiven.io/docs/products/mysql/backups-migration` (and `howto/track-restore-progress.md` to monitor a restore). Because these procedures vary by plan, use the official guide rather than this document for the click-path.

**Before any restore:** make sure the current data is exported (command above). Restoring replaces the database contents — never experiment on your only copy.

## 23. Redeploy after GitHub updates

1. Commit and push to GitHub (with your approval — nothing is pushed automatically).
2. **Render**: auto-deploys by default on every push (configure **Settings → Auto-Deploy**: `On Commit` / `After CI Checks Pass` / off). Watch the **Deploys** page; new Flyway migrations (e.g. `V3__...`) apply automatically and existing data is untouched.
3. **Vercel**: auto-deploys by default; if you changed any `VITE_` variable, also redeploy so it gets baked in (§13).
4. Skip a deploy for docs-only commits by putting `deploy` or `cd` in the commit message (Render's skip phrase).
5. If a deploy breaks: roll back (§25).

## 24. Troubleshooting common errors

| Symptom | Cause | Fix |
|---|---|---|
| Backend crash: `JWT secret must be ... at least 32 characters` | `JWT_SECRET` missing/short on Render | Add a ≥32-char secret (§9), save, redeploy |
| Crash: `Could not install the Aiven CA truststore from /etc/secrets/aiven-ca.pem` | PEM pasted incompletely or corrupted | Re-paste the whole file including BEGIN/END lines (§10.9); check the startup log line about the Aiven CA |
| Log shows `No Aiven CA certificate at /etc/secrets/aiven-ca.pem` | Secret file missing → `VERIFY_CA` will then fail at connection time | Create the secret file with the exact filename `aiven-ca.pem` |
| `Public Key Retrieval is not allowed` or SSL errors | `MYSQL_URL` lost its TLS settings | Restore `?sslMode=VERIFY_CA&serverTimezone=UTC` (§7) |
| `Access denied for user 'avnadmin'` | wrong username/password | Re-copy from Aiven **Connection information** (§5) |
| `Communications link failure` / connection timeouts | wrong host/port, or DB not `Running` | Verify host/port; check Aiven service status |
| Startup fails with Hibernate *schema validation* error | entities and DB schema out of sync | Do **not** switch to `create`/`update`. Check Flyway logs; add a new `V3__` migration if a change is missing |
| Browser: *blocked by CORS policy* | `CORS_ALLOWED_ORIGINS` mismatch | Exact `https://` origin, no trailing slash, then save + redeploy (§14) |
| All API calls return **401** after you changed `JWT_SECRET` | existing tokens were signed with the old secret | Expected — users simply log in again |
| Frontend refresh on a deep link → 404 | wrong Vercel Root Directory or missing rewrite | Root Directory = `frontend`; `frontend/vercel.json` must be deployed (§12) |
| Frontend loads but every API call hits `*.vercel.app/api` → 404 | `VITE_API_BASE_URL` not set at build time | Add it (§13) and **redeploy** |
| Render health check failing | wrong health path or app not starting | **Health Check Path** = `/actuator/health`; read **Logs** |
| First request takes ~1 minute | Render Free plan spin-down (§26) | Expected on Free; upgrade to remove it |
| SSE dies after ~15 min idle (Free plan) | instance spun down; client auto-reconnects | Expected on Free; upgrade for always-on |
| Razorpay webhook returns 400 | `RAZORPAY_WEBHOOK_SECRET` mismatch | Make the Render value identical to the Razorpay one (§16) |
| `Too many connections` from MySQL | pool too large for the plan | Lower `DB_POOL_MAX_SIZE` |
| Docker build fails | wrong Dockerfile path | **Dockerfile Path** = `backend/Dockerfile` |

## 25. Roll back a failed deployment

**Render (backend):**

1. Dashboard → your service → **Deploys** page.
2. Find a recent **successful** deploy → click **Rollback**.
3. On the confirmation page click **Rollback to this deploy**.
4. Render reuses that deploy's build artifact (fast) — environment variables from the *target deploy* are used for the rollback only; your current settings otherwise stay.
5. Rollback **auto-disables auto-deploys** so the bad commit can't re-deploy itself. After fixing, re-enable from the service's **Settings** page (**Auto-Deploy**).
6. Limitation: only the **two most recent previous deploys** are available on the Free plan.

**Vercel (frontend):**

1. Project → **Deployments** page.
2. Open the ⋯ menu of the last known-good deployment and promote it to production (Vercel labels this **Promote to Production**).

**Database:** Flyway migrations are forward-only — rolling back app code does **not** roll back schema changes. This is safe for FitGear's migrations (V1/V2 are additive tables), but keep it in mind for future migrations: prefer additive `V3__` changes so old code keeps working.

## 26. Expected costs and free-tier limitations (verified)

**Render Free plan** (official documentation, verified):

* **750 free instance hours per workspace per calendar month** (reset monthly, don't roll over; exhausted → free services suspended until next month).
* **Spins down after 15 minutes** without inbound traffic; next request spins it up in **~1 minute** (a loading page appears meanwhile). *Impacts: first visit is slow; idle SSE streams end and reconnect.*
* **Ephemeral filesystem** — anything written to disk is lost on redeploy/restart/spin-down. (FitGear is safe: all data is in Aiven.)
* **No persistent disks** on Free.
* Counts against your monthly **outbound bandwidth** and **build pipeline minutes**; exceeding them can suspend free services for the month.
* **Rollbacks limited to the two most recent previous deploys.**
* Render's own wording: *"Do not use them for production applications"* — fine for testing and hobby projects; upgrade when you need reliability.

**Aiven:** paid plans only (prices vary by cloud/region/plan — check aiven.io/pricing). Daily automatic backups are included; retention depends on the plan. The cheapest suitable plan is enough for FitGear; keep `DB_POOL_MAX_SIZE` below the plan's connection limit.

**Vercel:** free "Hobby" tier exists for personal projects — current limits at vercel.com/pricing (they change; check before relying on them).

**Razorpay:** test mode is free. Live mode charges per-transaction fees — see razorpay.com/pricing. FitGear never enables live credentials by itself.

**GitHub:** any plan works; private repos are fine.

---

## Appendix A — Environment variable reference

**Render (backend):** `SPRING_PROFILES_ACTIVE`, `MYSQL_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `CORS_ALLOWED_ORIGINS`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, `SEED_PRODUCTS`, `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`, `AIVEN_CA_PEM_PATH` (optional), plus Render's own `PORT` (auto-injected — don't set).

**Vercel (frontend):** `VITE_API_BASE_URL` (public, baked at build time — never a secret).

**Local development:** `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` for the backend (see README); everything else defaults. H2 in-memory database — no setup needed.

**Never in Git:** anything from the Secret column of §9, `.env` files, `ca.pem`, `*.p12`, `*.jks` (all ignored by `.gitignore`/`.dockerignore`).

## Appendix B — Deployment checklist (print this)

- [ ] 1. Code pushed to GitHub *(your approval)*
- [ ] 2. Aiven service **Running** (§3); host/port/password copied (§5); CA downloaded (§6)
- [ ] 3. Render web service created: Language **Docker**, Dockerfile Path `backend/Dockerfile`, Health Check `/actuator/health` (§10)
- [ ] 4. All Render env vars added, secrets marked Secret (§9); secret file `aiven-ca.pem` added
- [ ] 5. Health endpoint returns `"status":"UP"` (§11)
- [ ] 6. Vercel project created: Root Directory `frontend`, `VITE_API_BASE_URL` set, deploy succeeds (§12–13)
- [ ] 7. `CORS_ALLOWED_ORIGINS` = Vercel origin on Render + redeploy (§14)
- [ ] 8. Razorpay test keys + webhook configured (§15–16)
- [ ] 9. Flyway: 2 migrations, both `success=1` (§17)
- [ ] 10. Customer checklist (§18), admin checklist (§19), SSE (§20), restart persistence (§21)

## Appendix C — What was verified during preparation vs. what remains manual

**Verified locally by running the real thing:**

* Backend: full Maven test suite passes — **67 tests, 0 failures** (including security/RBAC flow, payments, SSE, and new truststore tests).
* Production (`mysql`) profile starts, binds to the injected `PORT` value, runs Flyway, serves `/actuator/health` (`status: UP`), returns products publicly, returns **401** for `/api/cart`, `/api/orders`, `/api/admin/dashboard` without tokens.
* CORS preflight: allowed origin → `Access-Control-Allow-Origin` echoed with `Authorization` allowed; disallowed origin → header absent.
* Frontend: `oxlint` passes (3 pre-existing warnings), `vite build` succeeds, and `VITE_API_BASE_URL` is confirmed present in the built bundle.
* Truststore installer: PEM → merged PKCS12 (Aiven CA + 118 JVM anchors) round-trips; malformed PEM fails closed; pre-set truststore respected.
* Aiven/Render/Vercel/Razorpay documentation facts in this guide were taken from the platforms' official docs (field names quoted verbatim).

**Cannot be verified from this machine — must be confirmed by you (nothing here is claimed as tested):**

* The live Aiven connection (no credentials available) — first Render deploy proves it; watch for the `Successfully applied 2 migrations` log line.
* Docker image build (Docker is not installed on this machine) — Render builds it remotely; the Dockerfile follows the verified Render spec.
* Actual Render/Vercel/Aiven dashboard clicks and any plan-specific limits.
* Real Razorpay test payments and webhook delivery.
* Cross-origin SSE end-to-end between the real Vercel and Render URLs.

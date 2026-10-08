import axios from 'axios'

const baseURL =
  import.meta.env.VITE_API_URL ||
  (import.meta.env.DEV ? 'http://localhost:8080/api' : '/api')

const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('fitgear_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Global 401 handling: an expired/invalid token signs the user out.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const hadToken = Boolean(localStorage.getItem('fitgear_token'))
    if (status === 401 && hadToken) {
      localStorage.removeItem('fitgear_token')
      localStorage.removeItem('fitgear_user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

/** Extract a friendly message from an API error. */
export function errorMessage(error, fallback = 'Something went wrong. Please try again.') {
  return error?.response?.data?.message || error?.message || fallback
}

export { api, baseURL }

// ---------------------------------------------------------------------------
// Auth
// ---------------------------------------------------------------------------

export async function registerUser(details) {
  await api.post('/auth/register', details)
}

export async function loginUser(credentials) {
  const response = await api.post('/auth/login', credentials)
  return response.data
}

export async function fetchProfile() {
  const response = await api.get('/auth/me')
  return response.data
}

export async function updateProfile(details) {
  const response = await api.put('/auth/me', details)
  return response.data
}

// ---------------------------------------------------------------------------
// Products (public reads, admin writes)
// ---------------------------------------------------------------------------

/** params: { search, category, minPrice, maxPrice, page, size, sort } */
export async function fetchProducts(params = {}) {
  const response = await api.get('/products', { params })
  return response.data
}

export async function fetchProduct(productId) {
  const response = await api.get(`/products/${productId}`)
  return response.data
}

export async function fetchCategories() {
  const response = await api.get('/categories')
  return response.data
}

/** Loads every product page for the admin table (bounded loop). */
export async function fetchAdminProducts() {
  const products = []
  const maxPages = 20
  let page = 0
  let lastPage = false

  while (!lastPage && page < maxPages) {
    const response = await api.get('/products', {
      params: { page, size: 100, sort: 'id,desc' },
    })
    products.push(...(response.data.content || []))
    lastPage = response.data.last
    page += 1
  }

  return products
}

export async function createProduct(product) {
  const response = await api.post('/products', product)
  return response.data
}

export async function updateProduct(productId, product) {
  const response = await api.put(`/products/${productId}`, product)
  return response.data
}

export async function deleteProduct(productId) {
  await api.delete(`/products/${productId}`)
}

// ---------------------------------------------------------------------------
// Cart (server-computed totals)
// ---------------------------------------------------------------------------

export async function fetchCart() {
  const response = await api.get('/cart')
  return response.data
}

export async function addCartItem(productId, quantity = 1) {
  const response = await api.post('/cart/items', { productId, quantity })
  return response.data
}

export async function updateCartItem(itemId, quantity) {
  const response = await api.put(`/cart/items/${itemId}`, { quantity })
  return response.data
}

export async function removeCartItem(itemId) {
  await api.delete(`/cart/items/${itemId}`)
}

export async function clearCartRequest() {
  await api.delete('/cart')
}

// ---------------------------------------------------------------------------
// Orders (customer)
// ---------------------------------------------------------------------------

export async function placeOrder(payload) {
  const response = await api.post('/orders', payload)
  return response.data
}

export async function fetchOrders() {
  const response = await api.get('/orders')
  return response.data
}

export async function fetchOrder(orderId) {
  const response = await api.get(`/orders/${orderId}`)
  return response.data
}

export async function cancelOrder(orderId) {
  const response = await api.patch(`/orders/${orderId}/cancel`)
  return response.data
}

// ---------------------------------------------------------------------------
// Payments
// ---------------------------------------------------------------------------

export async function fetchPaymentConfig() {
  const response = await api.get('/payments/config')
  return response.data
}

export async function createCheckout(orderId) {
  const response = await api.post('/payments/checkout', { orderId })
  return response.data
}

export async function verifyPayment(payload) {
  const response = await api.post('/payments/verify', payload)
  return response.data
}

// ---------------------------------------------------------------------------
// Reviews
// ---------------------------------------------------------------------------

/** params: { page, size, sort } — sorts allowed: createdAt, rating, id */
export async function fetchReviews(productId, params = {}) {
  const response = await api.get(`/products/${productId}/reviews`, { params })
  return response.data
}

export async function fetchReviewSummary(productId) {
  const response = await api.get(`/products/${productId}/reviews/summary`)
  return response.data
}

/**
 * The signed-in customer's own review. Returns null when they have not
 * written one (the server answers 204), which axios delivers as an empty body.
 */
export async function fetchMyReview(productId) {
  try {
    const response = await api.get('/reviews/mine', { params: { productId } })
    return response.data || null
  } catch (error) {
    if (error.response?.status === 204) return null
    throw error
  }
}

/** Creates the caller's review, or replaces it if they already have one. */
export async function submitReview(productId, payload) {
  const response = await api.post(`/products/${productId}/reviews`, payload)
  return response.data
}

export async function deleteReview(reviewId) {
  await api.delete(`/reviews/${reviewId}`)
}

// ---------------------------------------------------------------------------
// Admin
// ---------------------------------------------------------------------------

export async function fetchAdminDashboard() {
  const response = await api.get('/admin/dashboard')
  return response.data
}

/** params: { status?, page?, size? } */
export async function fetchAdminOrders(params = {}) {
  const response = await api.get('/admin/orders', { params })
  return response.data
}

export async function fetchAdminOrder(orderId) {
  const response = await api.get(`/admin/orders/${orderId}`)
  return response.data
}

export async function updateAdminOrderStatus(orderId, status) {
  const response = await api.patch(`/admin/orders/${orderId}/status`, { status })
  return response.data
}

import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('fitgear_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export async function registerUser(details) {
  await api.post('/auth/register', details)
}

export async function loginUser(credentials) {
  const response = await api.post('/auth/login', credentials)
  return response.data
}

export async function fetchProducts(search = '', category = '') {
  const response = await api.get('/products', {
    params: {
      search: search || undefined,
      category: category || undefined,
      page: 0,
      size: 12,
      sort: 'name,asc',
    },
  })

  return response.data.content || []
}

export async function fetchAdminProducts() {
  const products = []
  let page = 0
  let lastPage = false

  while (!lastPage) {
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

export async function fetchCart() {
  const response = await api.get('/cart/items')
  return response.data
}

export async function addCartItem(productId, quantity = 1) {
  const response = await api.post('/cart/items', { productId, quantity })
  return response.data
}

export async function removeCartItem(itemId) {
  await api.delete(`/cart/items/${itemId}`)
}

export async function placeOrder() {
  const response = await api.post('/orders')
  return response.data
}

export async function fetchOrders() {
  const response = await api.get('/orders')
  return response.data
}

export async function cancelOrder(orderId) {
  const response = await api.patch(`/orders/${orderId}/cancel`)
  return response.data
}

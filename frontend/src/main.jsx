import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import { AuthProvider } from './context/AuthContext.jsx'
import { CartProvider } from './context/CartContext.jsx'
import { ToastProvider } from './context/ToastContext.jsx'
import ProtectedRoute from './pages/ProtectedRoute.jsx'
import HomePage from './pages/HomePage.jsx'
import ProductsPage from './pages/ProductsPage.jsx'
import ProductDetailPage from './pages/ProductDetailPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import CartPage from './pages/CartPage.jsx'
import CheckoutPage from './pages/CheckoutPage.jsx'
import OrdersPage from './pages/OrdersPage.jsx'
import OrderDetailPage from './pages/OrderDetailPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import AdminDashboardPage from './pages/AdminDashboardPage.jsx'
import AdminProductsPage from './pages/AdminProductsPage.jsx'
import AdminOrdersPage from './pages/AdminOrdersPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <CartProvider>
            <Routes>
              <Route element={<App />}>
                <Route path="/" element={<HomePage />} />
                <Route path="/products" element={<ProductsPage />} />
                <Route path="/products/:productId" element={<ProductDetailPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/cart" element={<CartPage />} />
                <Route
                  path="/checkout"
                  element={<ProtectedRoute><CheckoutPage /></ProtectedRoute>}
                />
                <Route
                  path="/orders"
                  element={<ProtectedRoute><OrdersPage /></ProtectedRoute>}
                />
                <Route
                  path="/orders/:orderId"
                  element={<ProtectedRoute><OrderDetailPage /></ProtectedRoute>}
                />
                <Route
                  path="/profile"
                  element={<ProtectedRoute><ProfilePage /></ProtectedRoute>}
                />
                <Route
                  path="/admin"
                  element={<ProtectedRoute requiredRole="ADMIN"><AdminDashboardPage /></ProtectedRoute>}
                />
                <Route
                  path="/admin/products"
                  element={<ProtectedRoute requiredRole="ADMIN"><AdminProductsPage /></ProtectedRoute>}
                />
                <Route
                  path="/admin/orders"
                  element={<ProtectedRoute requiredRole="ADMIN"><AdminOrdersPage /></ProtectedRoute>}
                />
                <Route path="*" element={<NotFoundPage />} />
              </Route>
            </Routes>
          </CartProvider>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)

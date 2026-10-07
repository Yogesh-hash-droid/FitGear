import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'
import StatusBadge from '../components/StatusBadge'
import { errorMessage, fetchAdminDashboard } from '../services/api'
import { formatDate, formatPrice } from '../utils/format'
import './AdminPages.css'

export default function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    fetchAdminDashboard()
      .then((data) => {
        if (active) setDashboard(data)
      })
      .catch((requestError) => {
        if (active) setError(errorMessage(requestError, 'Could not load the dashboard.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  const metrics = dashboard
    ? [
        { label: 'Total orders', value: dashboard.totalOrders },
        { label: 'Revenue', value: formatPrice(dashboard.totalRevenue) },
        { label: 'Products', value: dashboard.totalProducts },
        { label: 'Low stock', value: dashboard.lowStockProducts },
        { label: 'Customers', value: dashboard.totalCustomers },
      ]
    : []

  return (
    <div className="site-shell">
      <Navbar />

      <section className="admin-content">
        <header className="admin-heading">
          <div>
            <p className="eyebrow">FITGEAR ADMIN</p>
            <h1>Control<br /><em>room.</em></h1>
          </div>
          <div className="admin-heading-links">
            <p>Store performance at a glance, then jump into products or order fulfilment.</p>
            <nav className="admin-subnav">
              <Link to="/admin">Dashboard</Link>
              <Link to="/admin/products">Products</Link>
              <Link to="/admin/orders">Orders</Link>
            </nav>
          </div>
        </header>

        {loading ? (
          <p className="admin-empty">Loading dashboard...</p>
        ) : error ? (
          <p className="admin-message admin-error" role="alert">{error}</p>
        ) : (
          <>
            <div className="metric-grid">
              {metrics.map((metric) => (
                <div className="metric-card" key={metric.label}>
                  <span>{metric.label}</span>
                  <strong>{metric.value}</strong>
                </div>
              ))}
            </div>

            <div className="metric-grid">
              {Object.entries(dashboard.ordersByStatus || {}).map(([status, count]) => (
                <div className="metric-card metric-status" key={status}>
                  <StatusBadge status={status} />
                  <strong>{count}</strong>
                </div>
              ))}
            </div>

            <section className="admin-inventory" aria-labelledby="recent-heading">
              <div className="admin-section-title">
                <h2 id="recent-heading">Recent orders</h2>
                <Link className="admin-text-button" to="/admin/orders">View all orders →</Link>
              </div>
              {dashboard.recentOrders?.length ? (
                <div className="admin-table-wrap">
                  <table className="admin-table">
                    <thead>
                      <tr><th>Order</th><th>Customer</th><th>Date</th><th>Total</th><th>Status</th></tr>
                    </thead>
                    <tbody>
                      {dashboard.recentOrders.map((order) => (
                        <tr key={order.id}>
                          <td><Link to={`/admin/orders?order=${order.id}`}>#{order.id}</Link></td>
                          <td>{order.customerName}</td>
                          <td>{formatDate(order.createdAt)}</td>
                          <td>{formatPrice(order.total)}</td>
                          <td><StatusBadge status={order.status} /></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="admin-empty">No orders yet.</p>
              )}
            </section>
          </>
        )}
      </section>

      <Footer />
    </div>
  )
}

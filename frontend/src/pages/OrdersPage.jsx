import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'
import StatusBadge from '../components/StatusBadge'
import { useToast } from '../context/useToast'
import { cancelOrder, errorMessage, fetchOrders } from '../services/api'
import { PAYMENT_METHOD_LABELS, formatDate, formatPrice } from '../utils/format'

export default function OrdersPage() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const { push } = useToast()

  async function load() {
    setLoading(true)
    setError('')
    try {
      setOrders(await fetchOrders())
    } catch (requestError) {
      setError(errorMessage(requestError, 'Could not load your orders.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    fetchOrders()
      .then((data) => {
        if (active) setOrders(data)
      })
      .catch((requestError) => {
        if (active) setError(errorMessage(requestError, 'Could not load your orders.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  async function handleCancel(order) {
    if (!window.confirm(`Cancel order #${order.id}?`)) return
    setBusyId(order.id)
    try {
      await cancelOrder(order.id)
      push(`Order #${order.id} cancelled.`, 'info')
      await load()
    } catch (requestError) {
      push(errorMessage(requestError, 'Could not cancel this order.'), 'error')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="site-shell">
      <Navbar />

      <section className="simple-page-content orders-page">
        <p className="eyebrow">YOUR ORDERS</p>
        <h1>Every session,<br /><em>on record.</em></h1>

        {loading ? (
          <p className="empty-state">Loading your orders...</p>
        ) : error ? (
          <p className="form-error" role="alert">{error}</p>
        ) : !orders.length ? (
          <div className="empty-state">
            <p>No orders yet. The catalog is waiting.</p>
            <Link className="primary-button" to="/products">Browse equipment <span>↘</span></Link>
          </div>
        ) : (
          <div className="order-list">
            {orders.map((order) => (
              <article className="order-card" key={order.id}>
                <header className="order-card-head">
                  <div>
                    <Link to={`/orders/${order.id}`}><strong>Order #{order.id}</strong></Link>
                    <span>{formatDate(order.createdAt)}</span>
                  </div>
                  <StatusBadge status={order.status} />
                </header>

                <div className="order-card-items">
                  {(order.items || []).slice(0, 3).map((item) => (
                    <span key={item.productId}>{item.productName} × {item.quantity}</span>
                  ))}
                  {(order.items || []).length > 3 && (
                    <span>+{order.items.length - 3} more</span>
                  )}
                </div>

                <footer className="order-card-foot">
                  <span>{order.payment ? PAYMENT_METHOD_LABELS[order.payment.method] : '—'} · {formatPrice(order.total)}</span>
                  <div className="history-actions">
                    <Link className="back-link" to={`/orders/${order.id}`}>View →</Link>
                    {order.cancelEligible && (
                      <button
                        type="button"
                        disabled={busyId === order.id}
                        onClick={() => handleCancel(order)}
                      >
                        {busyId === order.id ? 'Cancelling…' : 'Cancel'}
                      </button>
                    )}
                  </div>
                </footer>
              </article>
            ))}
          </div>
        )}
      </section>

      <Footer />
    </div>
  )
}

import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'
import OrderTimeline from '../components/OrderTimeline'
import ProductImage from '../components/ProductImage'
import StatusBadge from '../components/StatusBadge'
import { useToast } from '../context/useToast'
import { cancelOrder, errorMessage, fetchOrder } from '../services/api'
import { PAYMENT_METHOD_LABELS, STATUS_LABELS, formatDate, formatPrice } from '../utils/format'

export default function OrderDetailPage() {
  const { orderId } = useParams()
  const location = useLocation()
  const { push } = useToast()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [live, setLive] = useState(false)

  const load = useCallback(async () => {
    try {
      setOrder(await fetchOrder(orderId))
      setError('')
    } catch (requestError) {
      setError(errorMessage(requestError, 'Could not load this order.'))
    } finally {
      setLoading(false)
    }
  }, [orderId])

  useEffect(() => {
    let active = true
    setLoading(true)
    fetchOrder(orderId)
      .then((data) => {
        if (active) {
          setOrder(data)
          setLive(true)
        }
      })
      .catch((requestError) => {
        if (active) setError(errorMessage(requestError, 'Could not load this order.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [orderId])

  // Live updates pushed from the shared SSE stream (wired in App.jsx).
  useEffect(() => {
    function onOrderEvent(event) {
      const detail = event.detail
      if (!detail || String(detail.orderId) !== String(orderId)) return
      push(`Order #${orderId}: ${STATUS_LABELS[detail.status] || detail.status}`, 'live')
      load()
    }
    window.addEventListener('fitgear:order-event', onOrderEvent)
    return () => window.removeEventListener('fitgear:order-event', onOrderEvent)
  }, [orderId, load, push])

  async function handleCancel() {
    if (!window.confirm(`Cancel order #${orderId}?`)) return
    setBusy(true)
    try {
      const updated = await cancelOrder(orderId)
      setOrder(updated)
      push(`Order #${orderId} cancelled.`, 'info')
    } catch (requestError) {
      push(errorMessage(requestError, 'Could not cancel this order.'), 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="site-shell">
      <Navbar />

      <section className="simple-page-content order-detail-page">
        <Link className="back-link" to="/orders">← All orders</Link>

        {loading ? (
          <p className="empty-state">Loading order...</p>
        ) : error || !order ? (
          <p className="form-error" role="alert">{error || 'Order not found.'}</p>
        ) : (
          <>
            <header className="order-detail-head">
              <div>
                <p className="eyebrow">ORDER #{order.id}</p>
                <h1>Order<br /><em>#{order.id}.</em></h1>
                <p className="order-dates">
                  Placed {formatDate(order.createdAt)} · Last updated {formatDate(order.updatedAt)}
                </p>
              </div>
              <div className="order-detail-status">
                <span className={live ? 'live-indicator' : ''}>
                  {live ? '● Live' : '○ Offline'}
                </span>
                <StatusBadge status={order.status} />
                {order.cancelEligible && (
                  <button className="remove-button" type="button" disabled={busy} onClick={handleCancel}>
                    {busy ? 'Cancelling…' : 'Cancel order'}
                  </button>
                )}
              </div>
            </header>

            {location.state?.confirmed && (
              <div className="confirmation">
                <h2>Order confirmed.</h2>
                <p>{order.payment?.method === 'COD' ? 'Pay on delivery — cash or UPI at your door.' : 'Payment received. We are on it.'}</p>
              </div>
            )}

            <div className="order-detail-layout">
              <div className="order-detail-main">
                <section className="detail-section" aria-labelledby="timeline-heading">
                  <h2 id="timeline-heading">Status</h2>
                  <OrderTimeline status={order.status} />
                </section>

                <section className="detail-section" aria-labelledby="items-heading">
                  <h2 id="items-heading">Items</h2>
                  <div className="order-items-table">
                    {(order.items || []).map((item) => (
                      <div className="order-item-row" key={item.productId}>
                        <div className="cart-thumb">
                          <ProductImage src={item.imageUrl} name={item.productName} />
                        </div>
                        <div>
                          <Link to={`/products/${item.productId}`}><strong>{item.productName}</strong></Link>
                          <span>{formatPrice(item.unitPrice)} × {item.quantity}</span>
                        </div>
                        <strong>{formatPrice(Number(item.unitPrice) * item.quantity)}</strong>
                      </div>
                    ))}
                  </div>
                </section>

                <section className="detail-section" aria-labelledby="address-heading">
                  <h2 id="address-heading">Shipping</h2>
                  {order.address ? (
                    <address>
                      <strong>{order.address.receiverName}</strong><br />
                      {order.address.line1}
                      {order.address.line2 ? <>, {order.address.line2}</> : null}<br />
                      {order.address.city}, {order.address.state} {order.address.pincode}<br />
                      {order.address.phone}
                    </address>
                  ) : (
                    <p className="empty-state">No address on file.</p>
                  )}
                </section>
              </div>

              <aside className="order-summary">
                <h2>Payment</h2>
                {order.payment ? (
                  <>
                    <div className="summary-line">
                      <span>Method</span>
                      <span>{PAYMENT_METHOD_LABELS[order.payment.method] || order.payment.method}</span>
                    </div>
                    <div className="summary-line">
                      <span>Status</span>
                      <span>{order.payment.status}</span>
                    </div>
                    {order.payment.razorpayPaymentId && (
                      <div className="summary-line">
                        <span>Payment ID</span>
                        <span className="mono">{order.payment.razorpayPaymentId}</span>
                      </div>
                    )}
                  </>
                ) : (
                  <p className="empty-state">No payment record.</p>
                )}
                <div className="summary-line"><span>Subtotal</span><span>{formatPrice(order.subtotal)}</span></div>
                <div className="summary-line"><span>Shipping</span><span>Free</span></div>
                <div className="summary-line summary-total"><span>Total</span><strong>{formatPrice(order.total)}</strong></div>
              </aside>
            </div>
          </>
        )}
      </section>

      <Footer />
    </div>
  )
}

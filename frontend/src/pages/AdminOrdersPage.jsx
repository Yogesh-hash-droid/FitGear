import { Fragment, useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'
import StatusBadge from '../components/StatusBadge'
import { errorMessage, fetchAdminOrder, fetchAdminOrders, updateAdminOrderStatus } from '../services/api'
import { useToast } from '../context/useToast'
import {
  NEXT_STATUSES,
  PAYMENT_METHOD_LABELS,
  STATUS_FLOW,
  formatDate,
  formatPrice,
} from '../utils/format'
import './AdminPages.css'

const STATUS_FILTERS = ['All', ...STATUS_FLOW, 'CANCELLED']

export default function AdminOrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [status, setStatus] = useState(searchParams.get('status') || 'All')
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [expandedId, setExpandedId] = useState(null)
  const [expandedOrder, setExpandedOrder] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const { push } = useToast()

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const params = { page, size: 20, sort: 'id,desc' }
      if (status && status !== 'All') params.status = status
      setData(await fetchAdminOrders(params))
    } catch (requestError) {
      setError(errorMessage(requestError, 'Could not load orders.'))
    } finally {
      setLoading(false)
    }
  }, [page, status])

  useEffect(() => {
    load()
  }, [load])

  // Deep link from the dashboard (?order=123) opens that order's detail.
  useEffect(() => {
    const orderId = searchParams.get('order')
    if (!orderId) return
    let active = true
    fetchAdminOrder(orderId)
      .then((order) => {
        if (!active) return
        setExpandedId(order.id)
        setExpandedOrder(order)
      })
      .catch(() => {})
    return () => {
      active = false
    }
  }, [searchParams])

  async function toggleDetail(order) {
    if (expandedId === order.id) {
      setExpandedId(null)
      setExpandedOrder(null)
      return
    }
    setExpandedId(order.id)
    setExpandedOrder(order)
    try {
      const detail = await fetchAdminOrder(order.id)
      setExpandedOrder(detail)
    } catch (requestError) {
      push(errorMessage(requestError, 'Could not load order detail.'), 'error')
    }
  }

  async function changeStatus(order, nextStatus) {
    setBusyId(order.id)
    try {
      await updateAdminOrderStatus(order.id, nextStatus)
      push(`Order #${order.id} moved to ${nextStatus}.`, 'success')
      await load()
      if (expandedId === order.id) {
        setExpandedOrder(await fetchAdminOrder(order.id))
      }
    } catch (requestError) {
      push(errorMessage(requestError, 'Could not update status.'), 'error')
    } finally {
      setBusyId(null)
    }
  }

  function selectStatus(next) {
    setStatus(next)
    setPage(0)
    const params = new URLSearchParams(searchParams)
    if (next && next !== 'All') params.set('status', next)
    else params.delete('status')
    params.delete('order')
    setSearchParams(params, { replace: true })
  }

  const totalPages = Math.max(1, data.totalPages || 1)

  return (
    <div className="site-shell">
      <Navbar />

      <section className="admin-content">
        <header className="admin-heading">
          <div>
            <p className="eyebrow">FITGEAR ADMIN</p>
            <h1>Order<br /><em>fulfilment.</em></h1>
          </div>
          <div className="admin-heading-links">
            <p>Move orders through their lifecycle: confirm, process, ship, deliver. Cancellation restores stock automatically.</p>
            <nav className="admin-subnav">
              <Link to="/admin">Dashboard</Link>
              <Link to="/admin/products">Products</Link>
              <Link to="/admin/orders">Orders</Link>
            </nav>
          </div>
        </header>

        <div className="filter-bar">
          <div className="filter-group" role="group" aria-label="Filter orders by status">
            {STATUS_FILTERS.map((option) => (
              <button
                key={option}
                type="button"
                className={status === option ? 'active' : ''}
                onClick={() => selectStatus(option)}
              >
                {option === 'All' ? 'All' : option.charAt(0) + option.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
          <button className="admin-text-button" type="button" onClick={load} disabled={loading}>
            {loading ? 'Refreshing…' : 'Refresh'}
          </button>
        </div>

        {error && <p className="admin-message admin-error" role="alert">{error}</p>}

        {loading ? (
          <p className="admin-empty">Loading orders...</p>
        ) : !data.content?.length ? (
          <p className="admin-empty">No orders with this status.</p>
        ) : (
          <div className="admin-table-wrap">
            <table className="admin-table admin-orders-table">
              <thead>
                <tr>
                  <th>Order</th>
                  <th>Customer</th>
                  <th>Date</th>
                  <th>Payment</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((order) => (
                  <Fragment key={order.id}>
                    <tr>
                      <td><button className="admin-link-button" type="button" onClick={() => toggleDetail(order)}>#{order.id}</button></td>
                      <td>{order.customerName}<small>{order.customerEmail}</small></td>
                      <td>{formatDate(order.createdAt)}</td>
                      <td>
                        {order.payment ? PAYMENT_METHOD_LABELS[order.payment.method] : '—'}
                        <small>{order.payment?.status}</small>
                      </td>
                      <td>{formatPrice(order.total)}</td>
                      <td><StatusBadge status={order.status} /></td>
                      <td>
                        <div className="admin-actions">
                          <select
                            value=""
                            disabled={busyId === order.id || NEXT_STATUSES[order.status]?.length === 0}
                            onChange={(event) => {
                              if (event.target.value) changeStatus(order, event.target.value)
                            }}
                            aria-label={`Change status for order ${order.id}`}
                          >
                            <option value="">
                              {NEXT_STATUSES[order.status]?.length ? 'Set status…' : 'No moves'}
                            </option>
                            {(NEXT_STATUSES[order.status] || []).map((next) => (
                              <option key={next} value={next}>
                                {next === 'CANCELLED' ? 'Cancel order' : `→ ${next}`}
                              </option>
                            ))}
                          </select>
                          <button type="button" onClick={() => toggleDetail(order)}>
                            {expandedId === order.id ? 'Hide' : 'Details'}
                          </button>
                        </div>
                      </td>
                    </tr>
                    {expandedId === order.id && (
                      <tr className="admin-order-detail-row">
                        <td colSpan={7}>
                          {expandedOrder?.id === order.id ? (
                            <div className="admin-order-detail">
                              <div>
                                <h3>Items</h3>
                                {(expandedOrder.items || []).map((item) => (
                                  <div className="admin-order-item" key={item.productId}>
                                    <span>{item.productName} × {item.quantity}</span>
                                    <span>{formatPrice(Number(item.unitPrice) * item.quantity)}</span>
                                  </div>
                                ))}
                                <div className="admin-order-item">
                                  <strong>Total</strong>
                                  <strong>{formatPrice(expandedOrder.total)}</strong>
                                </div>
                              </div>
                              <div>
                                <h3>Shipping</h3>
                                {expandedOrder.address ? (
                                  <address>
                                    <strong>{expandedOrder.address.receiverName}</strong><br />
                                    {expandedOrder.address.line1}<br />
                                    {expandedOrder.address.city}, {expandedOrder.address.state} {expandedOrder.address.pincode}<br />
                                    {expandedOrder.address.phone}
                                  </address>
                                ) : (
                                  <p>No address on file.</p>
                                )}
                              </div>
                              <div>
                                <h3>Payment</h3>
                                <p>
                                  {expandedOrder.payment
                                    ? `${PAYMENT_METHOD_LABELS[expandedOrder.payment.method]} · ${expandedOrder.payment.status}`
                                    : 'No payment record'}
                                </p>
                                {expandedOrder.payment?.razorpayPaymentId && (
                                  <p className="mono">ID: {expandedOrder.payment.razorpayPaymentId}</p>
                                )}
                                <p className="admin-order-timeline">
                                  Placed {formatDate(expandedOrder.createdAt)}<br />
                                  Updated {formatDate(expandedOrder.updatedAt)}
                                </p>
                              </div>
                            </div>
                          ) : (
                            <p>Loading detail…</p>
                          )}
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </tbody>
            </table>

            {totalPages > 1 && (
              <nav className="pagination" aria-label="Order pages">
                <button type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
                  ← Previous
                </button>
                <span>Page {page + 1} of {totalPages} · {data.totalElements} orders</span>
                <button
                  type="button"
                  disabled={page + 1 >= totalPages}
                  onClick={() => setPage((value) => value + 1)}
                >
                  Next →
                </button>
              </nav>
            )}
          </div>
        )}
      </section>

      <Footer />
    </div>
  )
}
